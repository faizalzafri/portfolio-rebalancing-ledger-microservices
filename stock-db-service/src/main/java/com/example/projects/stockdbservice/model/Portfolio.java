package com.example.projects.stockdbservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "name", nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "portfolio_target_allocation", joinColumns = @JoinColumn(name = "portfolio_id"))
    @MapKeyColumn(name = "asset_symbol")
    @Column(name = "target_weight")
    private Map<String, BigDecimal> targetAllocations = new HashMap<>();

    public Portfolio() {
    }

    public Portfolio(String username, String name, Map<String, BigDecimal> targetAllocations) {
        this.username = username;
        this.name = name;
        this.targetAllocations = targetAllocations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, BigDecimal> getTargetAllocations() {
        return targetAllocations;
    }

    public void setTargetAllocations(Map<String, BigDecimal> targetAllocations) {
        this.targetAllocations = targetAllocations;
    }
}
