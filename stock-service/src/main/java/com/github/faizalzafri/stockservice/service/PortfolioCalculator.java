package com.github.faizalzafri.stockservice.service;

import com.github.faizalzafri.stockservice.model.PortfolioDto;
import com.github.faizalzafri.stockservice.model.TaxLotDto;
import com.github.faizalzafri.stockservice.model.TradeSuggestionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PortfolioCalculator {

    private static final Logger log = LoggerFactory.getLogger(PortfolioCalculator.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockPriceService stockPriceService;

    // Thresholds: 5% allocation drift, 10% unrealized loss for tax harvesting
    private static final BigDecimal DRIFT_THRESHOLD = new BigDecimal("0.05");
    private static final BigDecimal TAX_HARVEST_THRESHOLD = new BigDecimal("0.10");

    public List<TradeSuggestionDto> analyzePortfolio(PortfolioDto portfolio) {
        log.info("Analyzing portfolio: {} (ID: {})", portfolio.getName(), portfolio.getId());

        // 1. Fetch active tax lots from db-service
        ResponseEntity<List<TaxLotDto>> response = restTemplate.exchange(
                "http://db-service/rest/db/portfolio/" + portfolio.getId() + "/lots",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<TaxLotDto>>() {});
        List<TaxLotDto> lots = response.getBody();

        if (lots == null || lots.isEmpty()) {
            log.info("No active tax lots in portfolio {}", portfolio.getId());
            return List.of();
        }

        // 2. Fetch prices and calculate current values per asset
        Map<String, BigDecimal> assetPrices = new HashMap<>();
        Map<String, BigDecimal> currentValues = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (TaxLotDto lot : lots) {
            String symbol = lot.getSymbol().toUpperCase();
            if (!assetPrices.containsKey(symbol)) {
                assetPrices.put(symbol, stockPriceService.getPrice(symbol));
            }
            BigDecimal price = assetPrices.get(symbol);
            BigDecimal lotValue = lot.getRemainingQuantity().multiply(price);
            
            currentValues.put(symbol, currentValues.getOrDefault(symbol, BigDecimal.ZERO).add(lotValue));
            totalValue = totalValue.add(lotValue);
        }

        log.info("Total Portfolio Value: ${}", totalValue);
        List<TradeSuggestionDto> suggestions = new ArrayList<>();

        if (totalValue.compareTo(BigDecimal.ZERO) <= 0) {
            return suggestions;
        }

        // 3. TAX-LOSS HARVESTING ANALYSIS
        for (TaxLotDto lot : lots) {
            BigDecimal price = assetPrices.get(lot.getSymbol().toUpperCase());
            BigDecimal costBasis = lot.getPurchasePrice();
            
            if (price.compareTo(costBasis) < 0) {
                // Calculate loss percentage: (Cost Basis - Current Price) / Cost Basis
                BigDecimal loss = costBasis.subtract(price);
                BigDecimal lossPct = loss.divide(costBasis, 4, RoundingMode.HALF_UP);
                
                if (lossPct.compareTo(TAX_HARVEST_THRESHOLD) >= 0) {
                    log.info("Tax-Loss Harvesting candidate found! Lot ID {} of {} has unrealized loss of {}%", 
                             lot.getId(), lot.getSymbol(), lossPct.multiply(BigDecimal.valueOf(100)));
                    
                    suggestions.add(new TradeSuggestionDto(
                            portfolio.getId(),
                            lot.getSymbol(),
                            "SELL",
                            lot.getRemainingQuantity(),
                            String.format("Tax-Loss Harvesting (Unrealized Loss: %.2f%%)", lossPct.doubleValue() * 100.0),
                            TradeSuggestionDto.SuggestionStatus.PENDING
                    ));
                }
            }
        }

        // 4. REBALANCING DRIFT ANALYSIS
        Map<String, BigDecimal> targetAllocations = portfolio.getTargetAllocations();
        for (Map.Entry<String, BigDecimal> entry : targetAllocations.entrySet()) {
            String symbol = entry.getKey().toUpperCase();
            BigDecimal targetWeight = entry.getValue();
            
            BigDecimal currentValue = currentValues.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal currentWeight = currentValue.divide(totalValue, 4, RoundingMode.HALF_UP);
            
            // Calculate absolute drift: |currentWeight - targetWeight|
            BigDecimal drift = currentWeight.subtract(targetWeight).abs();
            log.info("Asset {}: Target Weight = {}, Current Weight = {}, Drift = {}", 
                     symbol, targetWeight, currentWeight, drift);

            if (drift.compareTo(DRIFT_THRESHOLD) >= 0) {
                // Calculate buy/sell target value difference
                BigDecimal targetValue = totalValue.multiply(targetWeight);
                BigDecimal diffVal = targetValue.subtract(currentValue);
                BigDecimal price = assetPrices.get(symbol);
                
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sharesDiff = diffVal.divide(price, 4, RoundingMode.HALF_UP);
                    if (sharesDiff.compareTo(BigDecimal.ZERO) > 0) {
                        suggestions.add(new TradeSuggestionDto(
                                portfolio.getId(), symbol, "BUY", sharesDiff,
                                String.format("Rebalance (Drift: %.2f%%)", drift.doubleValue() * 100.0),
                                TradeSuggestionDto.SuggestionStatus.PENDING
                        ));
                    } else if (sharesDiff.compareTo(BigDecimal.ZERO) < 0) {
                        suggestions.add(new TradeSuggestionDto(
                                portfolio.getId(), symbol, "SELL", sharesDiff.abs(),
                                String.format("Rebalance (Drift: %.2f%%)", drift.doubleValue() * 100.0),
                                TradeSuggestionDto.SuggestionStatus.PENDING
                        ));
                    }
                }
            }
        }

        return suggestions;
    }
}
