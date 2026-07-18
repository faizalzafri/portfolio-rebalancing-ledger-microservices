package com.github.faizalzafri.rebalancing.controller;

import com.github.faizalzafri.rebalancing.client.DbServiceClient;
import com.github.faizalzafri.rebalancing.model.Quote;
import com.github.faizalzafri.rebalancing.service.StockPriceService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/stock")
public class StockResourceController {

	private static final Logger LOG = LoggerFactory.getLogger(StockResourceController.class);

	@Autowired
	private DbServiceClient dbServiceClient;

	@Autowired
	private StockPriceService stockPriceService;

	@GetMapping("/{username}")
	@CircuitBreaker(name="dbServiceCircuit", fallbackMethod = "dbServiceFallback")
	public List<Quote> getStock(@PathVariable("username") final String username) {
		LOG.info("Received request to fetch stock quotes for user {}", username);

		// Use Feign Client instead of RestTemplate
		List<String> quotes = dbServiceClient.getQuotes(username);
		
		if (CollectionUtils.isEmpty(quotes)) {
			LOG.info("No quotes found in portfolio-ledger-service for user {}", username);
			return List.of();
		}

		LOG.info("Retrieved {} quotes for user {} from portfolio-ledger-service. Retrieving market prices..", quotes.size(), username);

		return quotes.stream().map(quoteSymbol -> {
			BigDecimal price = stockPriceService.getPrice(quoteSymbol);
			return new Quote(quoteSymbol, price);
		}).collect(Collectors.toList());
	}

	// called when circuit is open or portfolio-ledger-service is down
	public List<Quote> dbServiceFallback(@PathVariable("username") final String username, Throwable ex) {
		LOG.error("portfolio-ledger-service is currently unavailable! Invoking fallback for user: {}. Error details: {}", username, ex.getMessage());

		// return a mock cached fallback response to keep the client functional
		return List.of(
				new Quote("FALLBACK-MSFT", BigDecimal.valueOf(420.59)),
				new Quote("FALLBACK-AAPL", BigDecimal.valueOf(185.75))
		);
	}
}
