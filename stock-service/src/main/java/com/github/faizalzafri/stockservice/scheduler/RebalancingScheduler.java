package com.github.faizalzafri.stockservice.scheduler;

import com.github.faizalzafri.stockservice.model.PortfolioDto;
import com.github.faizalzafri.stockservice.model.TradeSuggestionDto;
import com.github.faizalzafri.stockservice.service.PortfolioCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RebalancingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RebalancingScheduler.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PortfolioCalculator portfolioCalculator;

    // Runs once every 60 seconds (for testing and demonstration)
    @Scheduled(fixedDelay = 60000)
    public void runPeriodicAnalysis() {
        log.info("Starting background portfolio rebalancing and tax-loss harvesting analysis...");

        try {
            // 1. Retrieve all portfolios from db-service
            ResponseEntity<List<PortfolioDto>> portfoliosResponse = restTemplate.exchange(
                    "http://db-service/rest/db/portfolio/all",
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<PortfolioDto>>() {});
            List<PortfolioDto> portfolios = portfoliosResponse.getBody();

            if (portfolios == null || portfolios.isEmpty()) {
                log.info("No portfolios found to analyze");
                return;
            }

            for (PortfolioDto portfolio : portfolios) {
                // 2. Perform analysis
                List<TradeSuggestionDto> newSuggestions = portfolioCalculator.analyzePortfolio(portfolio);
                
                if (newSuggestions.isEmpty()) {
                    continue;
                }

                // 3. Deduplicate against existing PENDING suggestions
                ResponseEntity<List<TradeSuggestionDto>> pendingResponse = restTemplate.exchange(
                        "http://db-service/rest/db/suggestions/pending/" + portfolio.getId(),
                        HttpMethod.GET, null, new ParameterizedTypeReference<List<TradeSuggestionDto>>() {});
                List<TradeSuggestionDto> pending = pendingResponse.getBody();
                
                List<String> pendingKeys = pending == null ? List.of() : pending.stream()
                        .map(s -> s.getSymbol() + ":" + s.getAction())
                        .collect(Collectors.toList());

                List<TradeSuggestionDto> suggestionsToSave = newSuggestions.stream()
                        .filter(s -> !pendingKeys.contains(s.getSymbol() + ":" + s.getAction()))
                        .collect(Collectors.toList());

                if (!suggestionsToSave.isEmpty()) {
                    log.info("Saving {} new trade suggestions to database...", suggestionsToSave.size());
                    restTemplate.postForObject(
                            "http://db-service/rest/db/suggestions/add-all",
                            new HttpEntity<>(suggestionsToSave),
                            List.class
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error occurred during background analysis check", e);
        }
        
        log.info("Finished background portfolio analysis check.");
    }
}
