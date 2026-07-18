package com.github.faizalzafri.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.github.faizalzafri.ledger.repository")
@SpringBootApplication
public class PortfolioLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioLedgerApplication.class, args);
	}
}
