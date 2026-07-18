package com.github.faizalzafri.ledger.service;

import com.github.faizalzafri.ledger.model.TaxLot;
import com.github.faizalzafri.ledger.model.Transaction;
import com.github.faizalzafri.ledger.repository.TaxLotRepository;
import com.github.faizalzafri.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TaxLotRepository taxLotRepository;
    @InjectMocks
    private TransactionService transactionService;

    private Long portfolioId;
    private String symbol;
    private LocalDateTime timestamp;

    @BeforeEach
    public void setUp() {
        portfolioId = 1L;
        symbol = "MSFT";
        timestamp = LocalDateTime.now();
    }

    @Test
    public void postTransaction_Buy_CreatesNewTaxLot() {

        // Given
        BigDecimal price = BigDecimal.valueOf(400.0);
        BigDecimal quantity = BigDecimal.valueOf(10.0);

        Transaction mockTxn = new Transaction(portfolioId, symbol,
                Transaction.TransactionType.BUY, quantity, price, timestamp);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTxn);

        // when
        Transaction result = transactionService.postTransaction(portfolioId, symbol,
                Transaction.TransactionType.BUY, quantity, price, timestamp);

        // then
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        ArgumentCaptor<TaxLot> captor = ArgumentCaptor.forClass(TaxLot.class);
        verify(taxLotRepository, times(1)).save(captor.capture());

        TaxLot savedTaxLot = captor.getValue();
        assertNotNull(savedTaxLot);
        assertEquals(portfolioId, savedTaxLot.getPortfolioId());
        assertEquals(symbol, savedTaxLot.getSymbol());
        assertEquals(quantity, savedTaxLot.getOriginalQuantity());
        assertEquals(quantity, savedTaxLot.getRemainingQuantity());
        assertEquals(price, savedTaxLot.getPurchasePrice());
    }

    @Test
    public void postTransaction_Sell_DepletesSingleTaxLotFully() {
        // Given
        BigDecimal quantityToSell = new BigDecimal("4.0");
        BigDecimal price = new BigDecimal("410.00");

        TaxLot existingLot = new TaxLot(portfolioId, symbol, new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("390.00"), timestamp.minusDays(5));
        existingLot.setId(101L);
        List<TaxLot> activeLots = List.of(existingLot);

        Transaction mockTxn = new Transaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTxn);
        when(taxLotRepository.findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                portfolioId, symbol, BigDecimal.ZERO)).thenReturn(activeLots);

        // when
        Transaction result = transactionService.postTransaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);

        // then
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        ArgumentCaptor<TaxLot> taxLotCaptor = ArgumentCaptor.forClass(TaxLot.class);
        verify(taxLotRepository, times(1)).save(taxLotCaptor.capture());

        TaxLot updatedLot = taxLotCaptor.getValue();
        assertEquals(101L, updatedLot.getId());
        assertEquals(new BigDecimal("6.0"), updatedLot.getRemainingQuantity()); // 10.0 - 4.0
    }

    @Test
    public void postTransaction_Sell_DepletesMultipleTaxLotsFIFO() {
        // given
        BigDecimal quantityToSell = new BigDecimal("12.0");
        BigDecimal price = new BigDecimal("410.00");

        Transaction mockTxn = new Transaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);

        TaxLot oldestLot = new TaxLot(portfolioId, symbol, new BigDecimal("5.0"), new BigDecimal("5.0"), new BigDecimal("390.00"), timestamp.minusDays(10));
        oldestLot.setId(201L);
        TaxLot newerLot = new TaxLot(portfolioId, symbol, new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("395.00"), timestamp.minusDays(5));
        newerLot.setId(202L);

        List<TaxLot> activeLots = List.of(oldestLot, newerLot);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTxn);
        when(taxLotRepository.findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                portfolioId, symbol, BigDecimal.ZERO)).thenReturn(activeLots);

        // when
        Transaction result = transactionService.postTransaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);

        // then
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(Transaction.class));

        ArgumentCaptor<TaxLot> taxLotCaptor = ArgumentCaptor.forClass(TaxLot.class);
        verify(taxLotRepository, times(2)).save(taxLotCaptor.capture()); // Expecting save to be called twice

        List<TaxLot> savedLots = taxLotCaptor.getAllValues();

        // Oldest lot is fully depleted (remaining = 0)
        TaxLot savedOldest = savedLots.get(0);
        assertEquals(201L, savedOldest.getId());
        assertEquals(BigDecimal.ZERO, savedOldest.getRemainingQuantity());

        // Newer lot is partially depleted (remaining = 3.0)
        TaxLot savedNewer = savedLots.get(1);
        assertEquals(202L, savedNewer.getId());
        assertEquals(new BigDecimal("3.0"), savedNewer.getRemainingQuantity());
    }

    @Test
    public void postTransaction_Sell_ThrowsException_IfInsufficientShares() {
        // given
        BigDecimal quantityToSell = new BigDecimal("15.0");
        BigDecimal price = new BigDecimal("410.00");

        Transaction mockTxn = new Transaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTxn);

        // Total available is only 10 shares, but trying to sell 15
        TaxLot existingLot = new TaxLot(portfolioId, symbol, new BigDecimal("10.0"), new BigDecimal("10.0"), new BigDecimal("390.00"), timestamp.minusDays(5));
        existingLot.setId(301L);

        List<TaxLot> activeLots = List.of(existingLot);
        when(taxLotRepository.findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                portfolioId, symbol, BigDecimal.ZERO)).thenReturn(activeLots);

        // when
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.postTransaction(portfolioId, symbol, Transaction.TransactionType.SELL, quantityToSell, price, timestamp);
        });

        assertTrue(exception.getMessage().contains("Insufficient shares of MSFT in portfolio"));

        // then
        verify(taxLotRepository, times(1)).save(existingLot);
        assertEquals(BigDecimal.ZERO, existingLot.getRemainingQuantity());
    }


}
