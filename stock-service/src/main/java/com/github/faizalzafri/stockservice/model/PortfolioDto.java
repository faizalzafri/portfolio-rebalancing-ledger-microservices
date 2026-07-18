package com.github.faizalzafri.stockservice.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.Map;

public class PortfolioDto {
    private Long id;
    @NotBlank(message = "Username cannot be blank")
    private String username;
    @NotBlank(message = "Portfolio name cannot be blank")
    private String name;
    @NotEmpty(message = "Target allocations map cannot be empty")
    private Map<String, BigDecimal> targetAllocations;

    public PortfolioDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, BigDecimal> getTargetAllocations() { return targetAllocations; }
    public void setTargetAllocations(Map<String, BigDecimal> targetAllocations) { this.targetAllocations = targetAllocations; }
}
