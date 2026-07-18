package com.github.faizalzafri.ledger.controller;

import com.github.faizalzafri.ledger.model.*;
import com.github.faizalzafri.ledger.repository.PortfolioRepository;
import com.github.faizalzafri.ledger.repository.QuoteRepository;
import com.github.faizalzafri.ledger.repository.TaxLotRepository;
import com.github.faizalzafri.ledger.repository.TradeSuggestionRepository;
import com.github.faizalzafri.ledger.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/db")
@Tag(name = "Database Ledger API", description = "Endpoints for managing portfolios, tax lots, transactions, and trade suggestions")
public class DbServiceController {

    private final TransactionService transactionService;

    private final QuoteRepository quoteRepository;
    private final PortfolioRepository portfolioRepository;
    private final TaxLotRepository taxLotRepository;
    private final TradeSuggestionRepository tradeSuggestionRepository;

    public DbServiceController(QuoteRepository quoteRepository, TransactionService transactionService,
                               PortfolioRepository portfolioRepository, TaxLotRepository taxLotRepository,
                               TradeSuggestionRepository tradeSuggestionRepository) {
        this.quoteRepository = quoteRepository;
        this.transactionService = transactionService;
        this.portfolioRepository = portfolioRepository;
        this.taxLotRepository = taxLotRepository;
        this.tradeSuggestionRepository = tradeSuggestionRepository;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Get quote symbols registered by a user")
    public List<String> getQuotes(@PathVariable("username") String username) {
        return getQuotesByUsername(username);
    }

    private List<String> getQuotesByUsername(String username) {
        return quoteRepository.findByUsername(username)
                .stream()
                .map(Quote::getQuote)
                .collect(Collectors.toList());
    }

    @PostMapping("/add")
    @Operation(summary = "Add new stock quotes for a user")
    public List<String> addQuotes(@RequestBody Quotes quotes) {
        quotes.getQuotes()
                .stream()
                .forEach(quote -> quoteRepository.save(new Quote(quotes.getUsername(), quote)));

        return getQuotesByUsername(quotes.getUsername());
    }

    @PostMapping("/delete/{username}")
    @Operation(summary = "Delete all stock quotes registered for a user")
    public List<String> delete(@PathVariable("username") final String username) {
        List<Quote> quotes = quoteRepository.findByUsername(username);
        quoteRepository.deleteAll(quotes);

        return getQuotesByUsername(username);
    }

    @PostMapping("/transaction/add")
    @Operation(summary = "Post a new trade transaction (triggers FIFO tax-lot depletion on SELL)")
    public Transaction postTransaction(@Valid @RequestBody Transaction transactionRequest) {
        // Set execution timestamp if not provided
        if (transactionRequest.getTimestamp() == null) {
            transactionRequest.setTimestamp(LocalDateTime.now());
        }
        return transactionService.postTransaction(
                transactionRequest.getPortfolioId(),
                transactionRequest.getSymbol(),
                transactionRequest.getType(),
                transactionRequest.getQuantity(),
                transactionRequest.getPrice(),
                transactionRequest.getTimestamp()
        );
    }

    @GetMapping("/portfolio/all")
    @Operation(summary = "Fetch all portfolios (used by background rebalancing scheduler)")
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    @GetMapping("/portfolio/{id}")
    @Operation(summary = "Fetch a single portfolio by ID")
    public Portfolio getPortfolioById(@PathVariable("id") Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id));
    }

    @PostMapping("/portfolio/create")
    @Operation(summary = "Create a new portfolio with target allocations")
    public Portfolio createPortfolio(@Valid @RequestBody Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    // Fetch active tax lots for a portfolio (where remainingQuantity > 0)
    @GetMapping("/portfolio/{id}/lots")
    @Operation(summary = "Fetch all active tax lots for a portfolio (remaining quantity > 0)")
    public List<TaxLot> getActiveTaxLots(@PathVariable("id") Long portfolioId) {
        return taxLotRepository.findByPortfolioIdAndRemainingQuantityGreaterThan(
                portfolioId, BigDecimal.ZERO);
    }

    // Fetch pending trade suggestions to prevent duplicate recommendations
    @GetMapping("/suggestions/pending/{portfolioId}")
    @Operation(summary = "Fetch pending trade suggestions for a portfolio")
    public List<TradeSuggestion> getPendingSuggestions(@PathVariable("portfolioId") Long portfolioId) {
        return tradeSuggestionRepository.findByPortfolioIdAndStatus(
                portfolioId, TradeSuggestion.SuggestionStatus.PENDING);
    }

    // Persist new trade suggestions generated by the engine
    @PostMapping("/suggestions/add-all")
    @Operation(summary = "Persist a list of trade suggestions generated by the engine")
    public List<TradeSuggestion> saveSuggestions(@RequestBody List<TradeSuggestion> suggestions) {
        return tradeSuggestionRepository.saveAll(suggestions);
    }

    // Fetch a single trade suggestion by ID
    @GetMapping("/suggestions/{id}")
    @Operation(summary = "Fetch a single trade suggestion by ID")
    public TradeSuggestion getSuggestionById(@PathVariable("id") Long id) {
        return tradeSuggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found: " + id));
    }
}
