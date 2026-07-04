package com.example.projects.stockdbservice.controller;

import com.example.projects.stockdbservice.model.Quote;
import com.example.projects.stockdbservice.model.Quotes;
import com.example.projects.stockdbservice.model.Transaction;
import com.example.projects.stockdbservice.repository.QuoteRepository;
import com.example.projects.stockdbservice.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rest/db")
public class DbServiceController {

    private final QuoteRepository quoteRepository;
    private final TransactionService transactionService;

    public DbServiceController(QuoteRepository quoteRepository, TransactionService transactionService) {
        this.quoteRepository = quoteRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/{username}")
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
    public List<String> addQuotes(@RequestBody Quotes quotes) {

        quotes.getQuotes()
                .stream()
                .forEach(quote -> quoteRepository.save(new Quote(quotes.getUsername(), quote)));

        return getQuotesByUsername(quotes.getUsername());
    }

    @PostMapping("/delete/{username}")
    public List<String> delete(@PathVariable("username") final String username) {

        List<Quote> quotes = quoteRepository.findByUsername(username);
        quoteRepository.deleteAll(quotes);

        return getQuotesByUsername(username);
    }

    @PostMapping("/transaction/add")
    public Transaction postTransaction(@RequestBody Transaction transactionRequest) {
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
}
