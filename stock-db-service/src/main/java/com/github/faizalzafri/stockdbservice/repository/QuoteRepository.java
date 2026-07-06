package com.github.faizalzafri.stockdbservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.faizalzafri.stockdbservice.model.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

	List<Quote> findByUsername(String username);

}
