package com.example.tomatomall.Repository;

import com.example.tomatomall.po.Stockpile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockpileRepository extends JpaRepository<Stockpile, Integer> {
}
