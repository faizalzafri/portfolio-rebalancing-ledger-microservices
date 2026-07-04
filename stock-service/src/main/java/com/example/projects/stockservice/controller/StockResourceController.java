package com.example.projects.stockservice.controller;

import com.example.projects.stockservice.model.Quote;
import com.example.projects.stockservice.service.StockPriceService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/stock")
public class StockResourceController {

	private static final Logger LOG = LoggerFactory.getLogger(StockResourceController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private StockPriceService stockPriceService;

	@GetMapping("/{username}")
	@CircuitBreaker(name="dbServiceCircuit", fallbackMethod = "dbServiceFallback")
	public List<Quote> getStock(@PathVariable("username") final String username) {
		LOG.info("Received request to fetch stock quotes for user {}", username);

		ResponseEntity<List<String>> quoteResponse = restTemplate.exchange(
				"http://db-service/rest/db/" + username,
				HttpMethod.GET,
				null,
				new ParameterizedTypeReference<List<String>>() {});

		List<String> quotes = quoteResponse.getBody();
		if (CollectionUtils.isEmpty(quotes)) {
			LOG.info("No quotes found in db-service for user {}", username);
			return List.of();
		}

		LOG.info("Retrieved {} quotes for user {} from db-service. Retrieving market prices..", quotes.size(), username);

		return quotes.stream().map(quoteSymbol -> {
			BigDecimal price = stockPriceService.getPrice(quoteSymbol);
			return new Quote(quoteSymbol, price);
		}).collect(Collectors.toList());
	}

	// called when circuit is open or db-service is down
	public List<Quote> dbServiceFallback(@PathVariable("username") final String username, Throwable ex) {
		LOG.error("db-service is currently unavailable! Invoking fallback for user: {}. Error details: {}", username, ex.getMessage());

		// return a mock cached fallback response to keep the client functional
		return List.of(
				new Quote("FALLBACK-MSFT", BigDecimal.valueOf(420.59)),
				new Quote("FALLBACK-AAPL", BigDecimal.valueOf(185.75))
		);
	}
}
