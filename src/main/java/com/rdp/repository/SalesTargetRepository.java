package com.rdp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.SalesTarget;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {
    
    List<SalesTarget> findByYearAndMonthOrderByDay(Integer year, Integer month);
    
    Optional<SalesTarget> findByYearAndMonthAndDay(Integer year, Integer month, Integer day);
    
    @Query("SELECT st FROM SalesTarget st WHERE st.year = :year AND st.month = :month AND st.day <= :day ORDER BY st.day")
    List<SalesTarget> findTargetsUpToDay(@Param("year") Integer year, @Param("month") Integer month, @Param("day") Integer day);
    
    void deleteByYearAndMonth(Integer year, Integer month);
}
