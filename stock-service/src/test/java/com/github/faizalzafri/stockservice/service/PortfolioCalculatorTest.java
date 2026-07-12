package com.github.faizalzafri.stockservice.service;

import com.github.faizalzafri.stockservice.model.PortfolioDto;
import com.github.faizalzafri.stockservice.model.TaxLotDto;
import com.github.faizalzafri.stockservice.model.TradeSuggestionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PortfolioCalculatorTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StockPriceService stockPriceService;

    @InjectMocks
    private PortfolioCalculator portfolioCalculator;

    private PortfolioDto portfolio;
    private Long portfolioId;

    @BeforeEach
    public void setUp() {
        portfolioId = 1L;
        portfolio = new PortfolioDto();
        portfolio.setId(portfolioId);
        portfolio.setName("Retirement Portfolio");
        portfolio.setUsername("faiz");

        // Define target allocations: 60% MSFT, 40% AAPL
        Map<String, BigDecimal> targets = new HashMap<>();
        targets.put("MSFT", new BigDecimal("0.60"));
        targets.put("AAPL", new BigDecimal("0.40"));
        portfolio.setTargetAllocations(targets);
    }

    @Test
    public void analyzePortfolio_NoActiveLots_ReturnsEmptySuggestions() {
        // Given
        ResponseEntity<List<TaxLotDto>> emptyResponse = new ResponseEntity<>(List.of(), HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://db-service/rest/db/portfolio/1/lots"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(emptyResponse);

        // when
        List<TradeSuggestionDto> result = portfolioCalculator.analyzePortfolio(portfolio);

        // then
        assertTrue(result.isEmpty());
        verifyNoInteractions(stockPriceService);
    }

    @Test
    public void analyzePortfolio_NoDriftOrHarvest_ReturnsEmptySuggestions() {
        // given
        // Current prices: MSFT = $400, AAPL = $200
        when(stockPriceService.getPrice("MSFT")).thenReturn(new BigDecimal("400.00"));
        when(stockPriceService.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));


        // Current holdings total $4000:
        // MSFT: 6 shares @ $400 = $2400 (60% of $4000)
        // AAPL: 8 shares @ $200 = $1600 (40% of $4000)
        TaxLotDto lot1 = new TaxLotDto();
        lot1.setSymbol("MSFT");
        lot1.setRemainingQuantity(new BigDecimal("6.0"));
        lot1.setPurchasePrice(new BigDecimal("400.00"));

        TaxLotDto lot2 = new TaxLotDto();
        lot2.setSymbol("AAPL");
        lot2.setRemainingQuantity(new BigDecimal("8.0"));
        lot2.setPurchasePrice(new BigDecimal("200.00"));

        ResponseEntity<List<TaxLotDto>> responseEntity = new ResponseEntity<>(List.of(lot1, lot2), HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://db-service/rest/db/portfolio/1/lots"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // when
        List<TradeSuggestionDto> result = portfolioCalculator.analyzePortfolio(portfolio);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    public void analyzePortfolio_DriftTriggersRebalancingSuggestions() {
        // given
        // Current prices: MSFT = $400, AAPL = $200
        when(stockPriceService.getPrice("MSFT")).thenReturn(new BigDecimal("400.00"));
        when(stockPriceService.getPrice("AAPL")).thenReturn(new BigDecimal("200.00"));

        // Current holdings total $4000:
        // MSFT: 9 shares @ $400 = $3600 (90% of total) -> Drift of 30%
        // AAPL: 2 shares @ $200 = $400  (10% of total) -> Drift of 30%
        TaxLotDto lot1 = new TaxLotDto();
        lot1.setSymbol("MSFT");
        lot1.setRemainingQuantity(new BigDecimal("9.0"));
        lot1.setPurchasePrice(new BigDecimal("400.00"));

        TaxLotDto lot2 = new TaxLotDto();
        lot2.setSymbol("AAPL");
        lot2.setRemainingQuantity(new BigDecimal("2.0"));
        lot2.setPurchasePrice(new BigDecimal("200.00"));

        ResponseEntity<List<TaxLotDto>> responseEntity = new ResponseEntity<>(List.of(lot1, lot2), HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://db-service/rest/db/portfolio/1/lots"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // when
        List<TradeSuggestionDto> result = portfolioCalculator.analyzePortfolio(portfolio);

        // then
        assertEquals(2, result.size());

        // Target MSFT value = $4000 * 60% = $2400. Currently $3600. Diff = -$1200. Suggest SELL 3 shares.
        TradeSuggestionDto msftSuggestion = result.stream().filter(s -> s.getSymbol().equals("MSFT")).findFirst().orElseThrow();
        assertEquals("SELL", msftSuggestion.getAction());
        assertEquals(new BigDecimal("3.0000"), msftSuggestion.getQuantity());
        assertTrue(msftSuggestion.getReason().contains("Rebalance (Drift: 30.00%)"));

        // Target AAPL value = $4000 * 40% = $1600. Currently $400. Diff = +$1200. Suggest BUY 6 shares.
        TradeSuggestionDto aaplSuggestion = result.stream().filter(s -> s.getSymbol().equals("AAPL")).findFirst().orElseThrow();
        assertEquals("BUY", aaplSuggestion.getAction());
        assertEquals(new BigDecimal("6.0000"), aaplSuggestion.getQuantity());
        assertTrue(aaplSuggestion.getReason().contains("Rebalance (Drift: 30.00%)"));
    }

    @Test
    public void analyzePortfolio_LossTriggersTaxLossHarvestingSuggestions() {
        // given
        // Current prices: MSFT = $400, AAPL = $150
        when(stockPriceService.getPrice("MSFT")).thenReturn(new BigDecimal("400.00"));
        when(stockPriceService.getPrice("AAPL")).thenReturn(new BigDecimal("150.00"));

        // Current holdings (perfect 60/40 allocation to prevent rebalancing suggestions):
        // MSFT: 6 shares @ $400 = $2400 (60% of $4000 total)
        // AAPL: 10.6667 shares @ $150 = $1600 (40% of $4000 total)
        TaxLotDto lot1 = new TaxLotDto();
        lot1.setSymbol("MSFT");
        lot1.setRemainingQuantity(new BigDecimal("6.0"));
        lot1.setPurchasePrice(new BigDecimal("400.00"));

        // AAPL lot bought at $200. Current price $150. Loss = (200-150)/200 = 25% (exceeds 10% threshold)
        TaxLotDto lot2 = new TaxLotDto();
        lot2.setSymbol("AAPL");
        lot2.setRemainingQuantity(new BigDecimal("10.6667"));
        lot2.setPurchasePrice(new BigDecimal("200.00"));

        ResponseEntity<List<TaxLotDto>> responseEntity = new ResponseEntity<>(List.of(lot1, lot2), HttpStatus.OK);
        when(restTemplate.exchange(
                eq("http://db-service/rest/db/portfolio/1/lots"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // when
        List<TradeSuggestionDto> result = portfolioCalculator.analyzePortfolio(portfolio);

        // then: We only expect 1 trade suggestion (the tax harvest SELL on AAPL)
        assertEquals(1, result.size());
        TradeSuggestionDto harvestSuggestion = result.get(0);
        assertEquals("AAPL", harvestSuggestion.getSymbol());
        assertEquals("SELL", harvestSuggestion.getAction());
        assertEquals(new BigDecimal("10.6667"), harvestSuggestion.getQuantity());
        assertTrue(harvestSuggestion.getReason().contains("Tax-Loss Harvesting (Unrealized Loss: 25.00%)"));
    }



}
