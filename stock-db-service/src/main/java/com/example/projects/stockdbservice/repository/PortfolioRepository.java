package com.example.projects.stockdbservice.repository;

import com.example.projects.stockdbservice.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUsername(String username);
}
