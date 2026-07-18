package com.github.faizalzafri.stockservice.client;

import com.github.faizalzafri.stockservice.model.PortfolioDto;
import com.github.faizalzafri.stockservice.model.TaxLotDto;
import com.github.faizalzafri.stockservice.model.TradeSuggestionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "db-service")
public interface DbServiceClient {

    @PostMapping("/rest/db/portfolio/create")
    PortfolioDto createPortfolio(@RequestBody PortfolioDto portfolio);

    @GetMapping("/rest/db/portfolio/{id}")
    PortfolioDto getPortfolioById(@PathVariable("id") Long id);

    @GetMapping("/rest/db/portfolio/all")
    List<PortfolioDto> getAllPortfolios();

    @GetMapping("/rest/db/portfolio/{id}/lots")
    List<TaxLotDto> getActiveTaxLots(@PathVariable("id") Long id);

    @GetMapping("/rest/db/suggestions/pending/{portfolioId}")
    List<TradeSuggestionDto> getPendingSuggestions(@PathVariable("portfolioId") Long portfolioId);

    @PostMapping("/rest/db/suggestions/add-all")
    List<TradeSuggestionDto> saveSuggestions(@RequestBody List<TradeSuggestionDto> suggestions);

    @GetMapping("/rest/db/suggestions/{id}")
    TradeSuggestionDto getSuggestionById(@PathVariable("id") Long id);

    @PostMapping("/rest/db/transaction/add")
    Map<String, Object> postTransaction(@RequestBody Map<String, Object> transaction);

    @PostMapping("/rest/db/add")
    List<String> addQuotes(@RequestBody Map<String, Object> quotesRequest);

    @GetMapping("/rest/db/{username}")
    List<String> getQuotes(@PathVariable("username") String username);

    @PostMapping("/rest/db/delete/{username}")
    List<String> deleteQuotes(@PathVariable("username") String username);
}
