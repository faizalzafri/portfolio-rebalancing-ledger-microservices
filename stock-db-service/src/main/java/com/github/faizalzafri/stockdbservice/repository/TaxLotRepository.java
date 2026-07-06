package com.github.faizalzafri.stockdbservice.repository;

import com.github.faizalzafri.stockdbservice.model.TaxLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TaxLotRepository extends JpaRepository<TaxLot, Long> {
    // Sorts oldest tax lots first (critical for FIFO depletion)
    List<TaxLot> findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
            Long portfolioId, String symbol, BigDecimal zero);

    List<TaxLot> findByPortfolioIdAndRemainingQuantityGreaterThan(Long portfolioId, BigDecimal zero);
}
