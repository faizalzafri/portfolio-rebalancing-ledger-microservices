package com.github.faizalzafri.ledger.repository;

import com.github.faizalzafri.ledger.model.TradeSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TradeSuggestionRepository extends JpaRepository<TradeSuggestion, Long> {
    List<TradeSuggestion> findByPortfolioIdAndStatus(Long portfolioId, TradeSuggestion.SuggestionStatus status);
}
