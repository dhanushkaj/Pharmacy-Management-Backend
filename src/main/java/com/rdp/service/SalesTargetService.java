package com.rdp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.model.SalesTarget;
import com.rdp.repository.SalesTargetRepository;

@Service
public class SalesTargetService {

    @Autowired
    private SalesTargetRepository salesTargetRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<SalesTarget> getTargetsForMonth(Integer year, Integer month) {
        return salesTargetRepository.findByYearAndMonthOrderByDay(year, month);
    }

    public Optional<SalesTarget> getTargetForDay(Integer year, Integer month, Integer day) {
        return salesTargetRepository.findByYearAndMonthAndDay(year, month, day);
    }

    @Transactional
    public void saveTargetsForMonth(Integer year, Integer month, List<SalesTarget> targets) {
        // Delete existing targets for the month
        salesTargetRepository.deleteByYearAndMonth(year, month);
        
        // Save new targets
        for (SalesTarget target : targets) {
            target.setYear(year);
            target.setMonth(month);
            salesTargetRepository.save(target);
        }
    }

    @Transactional
    public SalesTarget saveOrUpdateTarget(Integer year, Integer month, Integer day, BigDecimal targetAmount) {
        Optional<SalesTarget> existing = salesTargetRepository.findByYearAndMonthAndDay(year, month, day);
        
        SalesTarget target;
        if (existing.isPresent()) {
            target = existing.get();
            target.setTargetAmount(targetAmount);
        } else {
            target = new SalesTarget();
            target.setYear(year);
            target.setMonth(month);
            target.setDay(day);
            target.setTargetAmount(targetAmount);
        }
        
        return salesTargetRepository.save(target);
    }

    /**
     * Get actual sales for a specific day from pharmacy.rdp_billings
     */
    public BigDecimal getActualSalesForDay(Integer year, Integer month, Integer day) {
        String sql = """
            SELECT COALESCE(SUM(grand_total), 0) 
            FROM pharmacy.rdp_billings 
            WHERE EXTRACT(YEAR FROM billing_date) = ? 
            AND EXTRACT(MONTH FROM billing_date) = ? 
            AND EXTRACT(DAY FROM billing_date) = ?
            """;
        
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, year, month, day);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get accumulated actual sales from day 1 to specified day
     */
    public BigDecimal getAccumulatedSalesUpToDay(Integer year, Integer month, Integer day) {
        String sql = """
            SELECT COALESCE(SUM(grand_total), 0) 
            FROM pharmacy.rdp_billings 
            WHERE EXTRACT(YEAR FROM billing_date) = ? 
            AND EXTRACT(MONTH FROM billing_date) = ? 
            AND EXTRACT(DAY FROM billing_date) <= ?
            """;
        
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, year, month, day);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get accumulated target from day 1 to specified day
     */
    public BigDecimal getAccumulatedTargetUpToDay(Integer year, Integer month, Integer day) {
        List<SalesTarget> targets = salesTargetRepository.findTargetsUpToDay(year, month, day);
        return targets.stream()
                .map(SalesTarget::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get dashboard data: today's sales vs target, and accumulated sales vs accumulated target
     */
    public Map<String, Object> getDashboardData() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        Map<String, Object> data = new HashMap<>();

        // Today's data
        BigDecimal todayTarget = getTargetForDay(year, month, day)
                .map(SalesTarget::getTargetAmount)
                .orElse(BigDecimal.ZERO);
        BigDecimal todaySales = getActualSalesForDay(year, month, day);

        data.put("todayTarget", todayTarget);
        data.put("todaySales", todaySales);
        data.put("todayDate", today.toString());
        data.put("todayDay", day);

        // Accumulated data (from day 1 to today)
        BigDecimal accumulatedTarget = getAccumulatedTargetUpToDay(year, month, day);
        BigDecimal accumulatedSales = getAccumulatedSalesUpToDay(year, month, day);

        data.put("accumulatedTarget", accumulatedTarget);
        data.put("accumulatedSales", accumulatedSales);
        data.put("currentMonth", month);
        data.put("currentYear", year);

        // Progress percentages
        if (todayTarget.compareTo(BigDecimal.ZERO) > 0) {
            data.put("todayProgress", todaySales.multiply(BigDecimal.valueOf(100)).divide(todayTarget, 2, BigDecimal.ROUND_HALF_UP));
        } else {
            data.put("todayProgress", BigDecimal.ZERO);
        }

        if (accumulatedTarget.compareTo(BigDecimal.ZERO) > 0) {
            data.put("accumulatedProgress", accumulatedSales.multiply(BigDecimal.valueOf(100)).divide(accumulatedTarget, 2, BigDecimal.ROUND_HALF_UP));
        } else {
            data.put("accumulatedProgress", BigDecimal.ZERO);
        }

        return data;
    }

    /**
     * Get daily breakdown for the current month (for chart display)
     */
    public List<Map<String, Object>> getMonthlyBreakdown(Integer year, Integer month) {
        // Get all targets for the month
        List<SalesTarget> targets = salesTargetRepository.findByYearAndMonthOrderByDay(year, month);
        
        // Get daily sales for the month
        String sql = """
            SELECT EXTRACT(DAY FROM billing_date)::INTEGER as day, 
                   COALESCE(SUM(grand_total), 0) as sales
            FROM pharmacy.rdp_billings 
            WHERE EXTRACT(YEAR FROM billing_date) = ? 
            AND EXTRACT(MONTH FROM billing_date) = ?
            GROUP BY EXTRACT(DAY FROM billing_date)
            ORDER BY day
            """;
        
        List<Map<String, Object>> salesData = jdbcTemplate.queryForList(sql, year, month);
        
        // Create a map of day -> sales
        Map<Integer, BigDecimal> salesByDay = new HashMap<>();
        for (Map<String, Object> row : salesData) {
            Integer day = ((Number) row.get("day")).intValue();
            BigDecimal sales = (BigDecimal) row.get("sales");
            salesByDay.put(day, sales);
        }
        
        // Create a map of day -> target
        Map<Integer, BigDecimal> targetByDay = new HashMap<>();
        for (SalesTarget target : targets) {
            targetByDay.put(target.getDay(), target.getTargetAmount());
        }
        
        // Build result for each day of the month
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", day);
            dayData.put("target", targetByDay.getOrDefault(day, BigDecimal.ZERO));
            dayData.put("sales", salesByDay.getOrDefault(day, BigDecimal.ZERO));
            result.add(dayData);
        }
        
        return result;
    }
}
