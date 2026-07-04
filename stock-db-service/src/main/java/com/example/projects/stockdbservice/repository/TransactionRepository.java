package com.example.projects.stockdbservice.repository;

import com.example.projects.stockdbservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByPortfolioIdAndSymbolOrderByTimestampAsc(Long portfolioId, String symbol);
    List<Transaction> findByPortfolioId(Long portfolioId);
}
