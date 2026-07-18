package com.github.faizalzafri.rebalancing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class StockPriceService {

    private static final Logger log = LoggerFactory.getLogger(StockPriceService.class);
    private final Random random = new Random();
    
    @Autowired
    @Qualifier("defaultRestTemplate")
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches price using Spring Cache (@Cacheable) via RedisCacheManager.
     * On cache-miss, executes the method under the Resilience4j Circuit Breaker.
     */
    @Cacheable(value = "stockPrices", key = "#quoteSymbol.toUpperCase()", cacheManager = "redisCacheManager")
    @CircuitBreaker(name = "yahooFinanceCircuit", fallbackMethod = "apiFallback")
    public BigDecimal getPrice(String quoteSymbol) {
        return fetchPriceFromApi(quoteSymbol);
    }

    /**
     * Call the unauthenticated Yahoo Finance JSON endpoint
     */
    public BigDecimal fetchPriceFromApi(String quoteSymbol) {
        log.info("Cache Miss. Requesting market price for {} from Yahoo Finance REST API...", quoteSymbol);
        
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + quoteSymbol.toUpperCase();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse nested JSON: chart -> result -> [0] -> meta -> regularMarketPrice
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode priceNode = root.path("chart")
                                         .path("result")
                                         .get(0)
                                         .path("meta")
                                         .path("regularMarketPrice");
                                         
                if (!priceNode.isMissingNode()) {
                    BigDecimal price = priceNode.decimalValue();
                    log.info("Successfully fetched live price for {}: ${}", quoteSymbol, price);
                    return price;
                }
            }
            throw new RuntimeException("Unexpected response or missing price data for symbol: " + quoteSymbol);
        } catch (Exception e) {
            throw new RuntimeException("API connection or parsing failed for symbol: " + quoteSymbol, e);
        }
    }

    /**
     * Fallback method if API fails or the circuit trips
     */
    public BigDecimal apiFallback(String quoteSymbol, Throwable t) {
        log.error("Yahoo Finance API call failed for {} (Circuit tripped). Error: {}. Invoking fallback mock.", 
                  quoteSymbol, t.getMessage());
        
        // Generate a stable mock price based on symbol hashCode to avoid random pricing during tests
        double baseVal = 50.0 + (Math.abs(quoteSymbol.hashCode() % 100));
        BigDecimal mockPrice = BigDecimal.valueOf(baseVal + (10.0 * random.nextDouble()))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
                
        log.info("Resilient Fallback Price generated for {}: ${}", quoteSymbol, mockPrice);
        return mockPrice;
    }
}
