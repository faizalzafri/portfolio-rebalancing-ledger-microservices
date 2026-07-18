package com.github.faizalzafri.ledger.service;

import com.github.faizalzafri.ledger.model.TaxLot;
import com.github.faizalzafri.ledger.model.Transaction;
import com.github.faizalzafri.ledger.repository.TaxLotRepository;
import com.github.faizalzafri.ledger.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TaxLotRepository taxLotRepository;

    public TransactionService(TransactionRepository transactionRepository, TaxLotRepository taxLotRepository) {
        this.transactionRepository = transactionRepository;
        this.taxLotRepository = taxLotRepository;
    }

    public Transaction postTransaction(Long portfolioId, String symbol, Transaction.TransactionType type,
                                       BigDecimal quantity, BigDecimal price, LocalDateTime timestamp) {

        log.info("Posting transaction: {} {} shares of {} at ${} in portfolio {}",
                type, quantity, symbol, price, portfolioId);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and price must be greater than zero");
        }

        Transaction transaction = new Transaction(portfolioId, symbol, type, quantity, price, timestamp);
        transaction =  transactionRepository.save(transaction);

        // process ledger
        if (type == Transaction.TransactionType.BUY) {
            // BUY creates a new tax lot containing the full quantity
            TaxLot taxLot = new TaxLot(portfolioId, symbol, quantity, quantity, price, timestamp);
            taxLotRepository.save(taxLot);
            log.info("Created new Tax Lot for {} (Qty: {})", symbol, quantity);
        } else if (type == Transaction.TransactionType.SELL) {
            // SELL depletes existing tax lot using FIFO logic
            depleteTaxLotsFIFO(portfolioId, symbol, quantity);
        }
        return transaction;
    }

    private void depleteTaxLotsFIFO(Long portfolioId, String symbol, BigDecimal quantityToSell) {
        List<TaxLot> activeLots = taxLotRepository
                .findByPortfolioIdAndSymbolAndRemainingQuantityGreaterThanOrderByPurchaseDateAsc(
                        portfolioId, symbol, BigDecimal.ZERO);
        log.debug("Found {} active tax lots for {} to deplete", activeLots.size(), symbol);

        BigDecimal remainingToSell = quantityToSell;

        for  (TaxLot lot: activeLots) {
            if (remainingToSell.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal lotRemaining = lot.getRemainingQuantity();
            if (lotRemaining.compareTo(remainingToSell) >= 0) {
                // This lot has enough shares to fully satisfy the remaining sale quantity
                BigDecimal newRemaining = lotRemaining.subtract(remainingToSell);
                lot.setRemainingQuantity(newRemaining);
                taxLotRepository.save(lot);
                log.info("Depleted {} shares from Tax Lot ID {}. Remaining in lot: {}",
                        remainingToSell, lot.getId(), newRemaining);
                remainingToSell = BigDecimal.ZERO;
            } else {
                // This lot is fully depleted, and we must proceed to the next oldest lot
                remainingToSell = remainingToSell.subtract(lotRemaining);
                lot.setRemainingQuantity(BigDecimal.ZERO);
                taxLotRepository.save(lot);
                log.info("Fully depleted Tax Lot ID {} of all {} shares.", lot.getId(), lotRemaining);
            }
        }

        // If after checking all active lots we still have remaining shares to sell, it's an illegal short-sale
        if (remainingToSell.compareTo(BigDecimal.ZERO) > 0) {
            log.error("Insufficient shares for {} in portfolio {}. Attempted to sell {}, missing {} shares.",
                    symbol, portfolioId, quantityToSell, remainingToSell);
            throw new IllegalArgumentException(
                    "Insufficient shares of " + symbol + " in portfolio to execute this sale. Missing: " + remainingToSell);
        }

    }
}
