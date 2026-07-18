package com.github.faizalzafri.rebalancing.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("yahooFinanceApi")
public class YahooFinanceHealthIndicator implements HealthIndicator {

    @Autowired
    @Qualifier("defaultRestTemplate")
    private RestTemplate restTemplate;

    @Override
    public Health health() {
        String testUrl = "https://query1.finance.yahoo.com/v8/finance/chart/AAPL";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("url", testUrl)
                        .withDetail("description", "Yahoo Finance Chart API is online and responding.")
                        .build();
            }
            return Health.down()
                    .withDetail("url", testUrl)
                    .withDetail("status", response.getStatusCode())
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("url", testUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
