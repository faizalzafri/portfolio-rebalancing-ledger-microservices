package com.example.projects.stockservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeSuggestionDto {
    public enum SuggestionStatus { PENDING, EXECUTED, CANCELLED }

    private Long id;
    private Long portfolioId;
    private String symbol;
    private String action;
    private BigDecimal quantity;
    private String reason;
    private SuggestionStatus status;
    private LocalDateTime createdAt;

    public TradeSuggestionDto() {}

    public TradeSuggestionDto(Long portfolioId, String symbol, String action, BigDecimal quantity, String reason, SuggestionStatus status) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.action = action;
        this.quantity = quantity;
        this.reason = reason;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPortfolioId() { return portfolioId; }
    public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
