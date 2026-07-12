package com.github.faizalzafri.stockdbservice;

import com.github.faizalzafri.stockdbservice.model.Portfolio;
import com.github.faizalzafri.stockdbservice.model.TaxLot;
import com.github.faizalzafri.stockdbservice.model.Transaction;
import com.github.faizalzafri.stockdbservice.repository.PortfolioRepository;
import com.github.faizalzafri.stockdbservice.repository.TaxLotRepository;
import com.github.faizalzafri.stockdbservice.repository.TransactionRepository;
import com.github.faizalzafri.stockdbservice.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // this uses Spring's TestContextManager which loads, manages, and caches the ApplicationContext
@ActiveProfiles("test") // Activates application-test.yml config
@Transactional // Automatically rolls back database transactions after each test runs
public class DbServiceIntegrationTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TaxLotRepository taxLotRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Test
    public void testCreatePortfolioAndSaveTargetAllocations() {
        // 1. Given: Prepare a portfolio with targets
        Map<String, BigDecimal> targets = new HashMap<>();
        targets.put("MSFT", new BigDecimal("0.60"));
        targets.put("AAPL", new BigDecimal("0.40"));
        
        Portfolio portfolio = new Portfolio("faiz", "Test Retirement Portfolio", targets);

        // 2. When:
        // ave portfolio to the in-memory H2 database
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        assertNotNull(savedPortfolio.getId());

        // 3. Then
        // Retrieve and verify the database saved targets correctly
        Portfolio retrieved = portfolioRepository.findById(savedPortfolio.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Test Retirement Portfolio", retrieved.getName());
        assertEquals("faiz", retrieved.getUsername());
        assertEquals(2, retrieved.getTargetAllocations().size());
        assertEquals(new BigDecimal("0.60"), retrieved.getTargetAllocations().get("MSFT"));
        assertEquals(new BigDecimal("0.40"), retrieved.getTargetAllocations().get("AAPL"));
    }

    @Test
    public void testTransactionService_FIFO_Depletion() {
        // 1. Given: Create a test portfolio in H2
        Portfolio portfolio = new Portfolio("faiz", "Trading Ledger Portfolio", new HashMap<>());
        portfolio = portfolioRepository.save(portfolio);
        Long portfolioId = portfolio.getId();

        LocalDateTime baseTime = LocalDateTime.now();

        // 2. When
        // Buy Lot 1: 10 shares of MSFT at $400.00 (oldest)
        transactionService.postTransaction(
                portfolioId, "MSFT", Transaction.TransactionType.BUY, 
                new BigDecimal("10.0"), new BigDecimal("400.00"), baseTime.minusDays(5)
        );

        // Buy Lot 2: 5 shares of MSFT at $410.00 (newer)
        transactionService.postTransaction(
                portfolioId, "MSFT", Transaction.TransactionType.BUY, 
                new BigDecimal("5.0"), new BigDecimal("410.00"), baseTime.minusDays(1)
        );

        // Then
        // Verify both tax lots exist in H2
        List<TaxLot> activeLots = taxLotRepository.findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                portfolioId, "MSFT", BigDecimal.ZERO
        );
        assertEquals(2, activeLots.size());
        assertEquals(0, new BigDecimal("10.0").compareTo(activeLots.get(0).getRemainingQuantity()));
        assertEquals(0, new BigDecimal("5.0").compareTo(activeLots.get(1).getRemainingQuantity()));

        // 3. When
        // Sell 12 shares of MSFT (triggers FIFO depletion across multiple lots)
        transactionService.postTransaction(
                portfolioId, "MSFT", Transaction.TransactionType.SELL, 
                new BigDecimal("12.0"), new BigDecimal("420.00"), baseTime
        );

        // 4. Then
        // Retrieve the active lots and assert FIFO remaining counts
        List<TaxLot> remainingLots = taxLotRepository.findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                portfolioId, "MSFT", BigDecimal.ZERO
        );
        
        // Only 1 lot should remain active (Lot 2 with 3 remaining shares)
        assertEquals(1, remainingLots.size());
        TaxLot activeLot2 = remainingLots.get(0);
        assertEquals(0, new BigDecimal("3.0").compareTo(activeLot2.getRemainingQuantity()));
        assertEquals(0, new BigDecimal("410.00").compareTo(activeLot2.getPurchasePrice())); // Verifies it was Lot 2
    }
}
