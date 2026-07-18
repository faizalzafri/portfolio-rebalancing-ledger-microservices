package com.github.faizalzafri.rebalancing.scheduler;

import com.github.faizalzafri.rebalancing.client.DbServiceClient;
import com.github.faizalzafri.rebalancing.model.PortfolioDto;
import com.github.faizalzafri.rebalancing.model.TradeSuggestionDto;
import com.github.faizalzafri.rebalancing.service.PortfolioCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RebalancingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RebalancingScheduler.class);

    @Autowired
    private DbServiceClient dbServiceClient;

    @Autowired
    private PortfolioCalculator portfolioCalculator;

    // Runs once every 60 seconds (for testing and demonstration)
    @Scheduled(fixedDelay = 60000)
    public void runPeriodicAnalysis() {
        log.info("Starting background portfolio rebalancing and tax-loss harvesting analysis...");

        try {
            // 1. Retrieve all portfolios from portfolio-ledger-service using Feign
            List<PortfolioDto> portfolios = dbServiceClient.getAllPortfolios();

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

                // 3. Deduplicate against existing PENDING suggestions using Feign
                List<TradeSuggestionDto> pending = dbServiceClient.getPendingSuggestions(portfolio.getId());
                
                List<String> pendingKeys = pending == null ? List.of() : pending.stream()
                        .map(s -> s.getSymbol() + ":" + s.getAction())
                        .collect(Collectors.toList());

                List<TradeSuggestionDto> suggestionsToSave = newSuggestions.stream()
                        .filter(s -> !pendingKeys.contains(s.getSymbol() + ":" + s.getAction()))
                        .collect(Collectors.toList());

                if (!suggestionsToSave.isEmpty()) {
                    log.info("Saving {} new trade suggestions to database...", suggestionsToSave.size());
                    dbServiceClient.saveSuggestions(suggestionsToSave);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred during background analysis check", e);
        }
        
        log.info("Finished background portfolio analysis check.");
    }
}
