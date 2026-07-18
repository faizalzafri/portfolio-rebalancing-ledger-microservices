package com.github.faizalzafri.stockservice.controller;

import com.github.faizalzafri.stockservice.model.PortfolioDto;
import com.github.faizalzafri.stockservice.model.TaxLotDto;
import com.github.faizalzafri.stockservice.model.TradeSuggestionDto;
import com.github.faizalzafri.stockservice.service.PortfolioCalculator;
import com.github.faizalzafri.stockservice.service.StockPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/rest/portfolio")
@Tag(name = "Portfolio Engine API", description = "Endpoints for portfolio creation, valuation reports, manual rebalancing, and trade execution")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PortfolioCalculator portfolioCalculator;

    @Autowired
    private StockPriceService stockPriceService;

    @PostMapping("/create")
    @Operation(summary = "Create a new portfolio with target allocations")
    public PortfolioDto createPortfolio(@Valid @RequestBody PortfolioDto portfolio) {
        log.info("Request to create portfolio: {}", portfolio.getName());
        return restTemplate.postForObject("http://db-service/rest/db/portfolio/create", portfolio, PortfolioDto.class);
    }

    @GetMapping("/{id}/report")
    @Operation(summary = "Generate a real-time valuation, drift, and tax-loss harvesting report")
    public Map<String, Object> getPortfolioReport(@PathVariable("id") Long id) {
        log.info("Generating real-time report for portfolio ID: {}", id);

        // Fetch Portfolio Metadata
        PortfolioDto portfolio = restTemplate.getForObject("http://db-service/rest/db/portfolio/" + id, PortfolioDto.class);
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio not found: " + id);
        }

        // Fetch active tax lots
        ResponseEntity<List<TaxLotDto>> lotsResponse = restTemplate.exchange(
                "http://db-service/rest/db/portfolio/" + id + "/lots",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<TaxLotDto>>() {});
        List<TaxLotDto> lots = lotsResponse.getBody();

        // Calculate holding values
        Map<String, BigDecimal> assetPrices = new HashMap<>();
        Map<String, BigDecimal> currentValues = new HashMap<>();
        Map<String, BigDecimal> currentQuantities = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        if (lots != null) {
            for (TaxLotDto lot : lots) {
                String symbol = lot.getSymbol().toUpperCase();
                if (!assetPrices.containsKey(symbol)) {
                    assetPrices.put(symbol, stockPriceService.getPrice(symbol));
                }
                BigDecimal price = assetPrices.get(symbol);
                BigDecimal lotValue = lot.getRemainingQuantity().multiply(price);

                currentValues.put(symbol, currentValues.getOrDefault(symbol, BigDecimal.ZERO).add(lotValue));
                currentQuantities.put(symbol, currentQuantities.getOrDefault(symbol, BigDecimal.ZERO).add(lot.getRemainingQuantity()));
                totalValue = totalValue.add(lotValue);
            }
        }

        // Compile holdings list
        List<Map<String, Object>> holdingsReport = new ArrayList<>();
        if (lots != null) {
            for (String symbol : currentValues.keySet()) {
                Map<String, Object> holding = new HashMap<>();
                BigDecimal val = currentValues.get(symbol);
                BigDecimal qty = currentQuantities.get(symbol);
                BigDecimal target = portfolio.getTargetAllocations().getOrDefault(symbol, BigDecimal.ZERO);
                BigDecimal actualWeight = totalValue.compareTo(BigDecimal.ZERO) > 0 ? 
                        val.divide(totalValue, 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

                holding.put("symbol", symbol);
                holding.put("quantity", qty);
                holding.put("marketPrice", assetPrices.get(symbol));
                holding.put("totalValue", val);
                holding.put("targetAllocation", target);
                holding.put("currentAllocation", actualWeight);
                holding.put("drift", actualWeight.subtract(target).abs());
                holdingsReport.add(holding);
            }
        }

        // Run engine calculations for suggestions
        List<TradeSuggestionDto> newSuggestions = portfolioCalculator.analyzePortfolio(portfolio);

        Map<String, Object> report = new HashMap<>();
        report.put("portfolioId", id);
        report.put("portfolioName", portfolio.getName());
        report.put("totalValue", totalValue);
        report.put("holdings", holdingsReport);
        report.put("suggestions", newSuggestions);

        return report;
    }

    @PostMapping("/{id}/rebalance")
    @Operation(summary = "Manually trigger rebalancing calculations and save generated suggestions")
    public List<TradeSuggestionDto> triggerManualRebalance(@PathVariable("id") Long id) {
        log.info("Manually triggering rebalancing analysis for portfolio ID: {}", id);

        PortfolioDto portfolio = restTemplate.getForObject("http://db-service/rest/db/portfolio/" + id, PortfolioDto.class);
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio not found: " + id);
        }

        List<TradeSuggestionDto> suggestions = portfolioCalculator.analyzePortfolio(portfolio);
        if (!suggestions.isEmpty()) {
            restTemplate.postForObject("http://db-service/rest/db/suggestions/add-all", suggestions, List.class);
        }
        return suggestions;
    }

    @PostMapping("/suggestions/{id}/execute")
    @Operation(summary = "Execute a trade suggestion (posts the transaction to the ledger and updates status to EXECUTED)")
    public Map<String, Object> executeTradeSuggestion(@PathVariable("id") Long id) {
        log.info("Executing trade suggestion ID: {}", id);

        // 1. Fetch trade suggestion from db-service
        TradeSuggestionDto suggestion = restTemplate.getForObject(
                "http://db-service/rest/db/suggestions/" + id, TradeSuggestionDto.class);
        
        if (suggestion == null) {
            throw new IllegalArgumentException("Suggestion not found: " + id);
        }

        if (suggestion.getStatus() != TradeSuggestionDto.SuggestionStatus.PENDING) {
            throw new IllegalStateException("Suggestion is already " + suggestion.getStatus());
        }

        // 2. Fetch current price of asset
        BigDecimal executionPrice = stockPriceService.getPrice(suggestion.getSymbol());

        // 3. Post transaction to ledger in db-service (BUY or SELL)
        Map<String, Object> txnRequest = new HashMap<>();
        txnRequest.put("portfolioId", suggestion.getPortfolioId());
        txnRequest.put("symbol", suggestion.getSymbol());
        txnRequest.put("type", suggestion.getAction());
        txnRequest.put("quantity", suggestion.getQuantity());
        txnRequest.put("price", executionPrice);
        txnRequest.put("timestamp", LocalDateTime.now().toString());

        log.info("Posting executed transaction to db-service ledger: {}", txnRequest);
        Map<?, ?> txnResult = restTemplate.postForObject(
                "http://db-service/rest/db/transaction/add", txnRequest, Map.class);

        // 4. Update suggestion status to EXECUTED
        suggestion.setStatus(TradeSuggestionDto.SuggestionStatus.EXECUTED);
        restTemplate.postForObject(
                "http://db-service/rest/db/suggestions/add-all", List.of(suggestion), List.class);

        Map<String, Object> response = new HashMap<>();
        response.put("suggestionId", id);
        response.put("status", "SUCCESS");
        response.put("executedPrice", executionPrice);
        response.put("transaction", txnResult);

        return response;
    }
}
