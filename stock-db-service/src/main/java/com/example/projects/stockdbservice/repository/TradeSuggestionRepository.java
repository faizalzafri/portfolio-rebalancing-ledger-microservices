package com.example.projects.stockdbservice.repository;

import com.example.projects.stockdbservice.model.TradeSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TradeSuggestionRepository extends JpaRepository<TradeSuggestion, Long> {
    List<TradeSuggestion> findByPortfolioIdAndStatus(Long portfolioId, TradeSuggestion.SuggestionStatus status);
}
