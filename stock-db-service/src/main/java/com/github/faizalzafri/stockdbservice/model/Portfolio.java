package com.github.faizalzafri.stockdbservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "portfolios")
public class Portfolio extends BaseEntity {

    @Column(name = "username", nullable = false)
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Portfolio name cannot be blank")
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "portfolio_target_allocation", joinColumns = @JoinColumn(name = "portfolio_id"))
    @MapKeyColumn(name = "asset_symbol")
    @Column(name = "target_weight")
    @NotEmpty(message = "Target allocations map cannot be empty")
    private Map<String, BigDecimal> targetAllocations = new HashMap<>();

    public Portfolio() {
    }

    public Portfolio(String username, String name, Map<String, BigDecimal> targetAllocations) {
        this.username = username;
        this.name = name;
        this.targetAllocations = targetAllocations;
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
