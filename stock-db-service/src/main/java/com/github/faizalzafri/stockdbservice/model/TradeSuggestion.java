package com.github.faizalzafri.stockdbservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_suggestions")
public class TradeSuggestion extends BaseEntity {

    public enum SuggestionStatus {
        PENDING, EXECUTED, CANCELLED
    }

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "action", nullable = false)
    private String action; // BUY or SELL

    @Column(name = "quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SuggestionStatus status;

    public TradeSuggestion() {}

    public TradeSuggestion(Long portfolioId, String symbol, String action, 
                           BigDecimal quantity, String reason, SuggestionStatus status) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.action = action;
        this.quantity = quantity;
        this.reason = reason;
        this.status = status;
    }

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
}
