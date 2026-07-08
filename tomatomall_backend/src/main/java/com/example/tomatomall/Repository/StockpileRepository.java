package com.example.tomatomall.Repository;

import com.example.tomatomall.po.Stockpile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockpileRepository extends JpaRepository<Stockpile, Integer> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Stockpile s SET s.amount = s.amount - :quantity, s.frozen = s.frozen + :quantity " +
            "WHERE s.id = :id AND s.amount >= :quantity")
    int decreaseStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}
