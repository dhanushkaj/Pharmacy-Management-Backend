package com.rdp.controller;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.Customer;
import com.rdp.repository.CustomerRepository;

@RestController
@RequestMapping("/api/customer-discount")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CustomerDiscountController {

    @Autowired
    private CustomerRepository customerRepository;

    // Store seasonal discounts in memory (in production, use database)
    private static Map<String, Map<String, Object>> seasonalDiscounts = new HashMap<>();

    static {
        initializeDefaultSeasonalDiscounts();
    }

    private static void initializeDefaultSeasonalDiscounts() {
        Map<String, Object> spring = new HashMap<>();
        spring.put("startMonth", 3);
        spring.put("startDay", 21);
        spring.put("endMonth", 6);
        spring.put("endDay", 20);
        spring.put("discountPercent", 0);
        seasonalDiscounts.put("SPRING", spring);

        Map<String, Object> summer = new HashMap<>();
        summer.put("startMonth", 6);
        summer.put("startDay", 21);
        summer.put("endMonth", 9);
        summer.put("endDay", 22);
        summer.put("discountPercent", 0);
        seasonalDiscounts.put("SUMMER", summer);

        Map<String, Object> fall = new HashMap<>();
        fall.put("startMonth", 9);
        fall.put("startDay", 23);
        fall.put("endMonth", 12);
        fall.put("endDay", 20);
        fall.put("discountPercent", 0);
        seasonalDiscounts.put("FALL", fall);

        Map<String, Object> winter = new HashMap<>();
        winter.put("startMonth", 12);
        winter.put("startDay", 21);
        winter.put("endMonth", 3);
        winter.put("endDay", 20);
        winter.put("discountPercent", 0);
        seasonalDiscounts.put("WINTER", winter);
    }

    /**
     * Get all seasonal discount configurations
     */
    @GetMapping("/seasonal")
    public ResponseEntity<?> getSeasonalDiscounts() {
        return ResponseEntity.ok(seasonalDiscounts);
    }

    /**
     * Save seasonal discount configurations
     */
    @PostMapping("/seasonal")
    public ResponseEntity<?> saveSeasonalDiscounts(@RequestBody Map<String, Map<String, Object>> discounts) {
        seasonalDiscounts = discounts;
        return ResponseEntity.ok(new HashMap<String, String>() {{
            put("message", "Seasonal discounts saved successfully");
        }});
    }

    /**
     * Apply seasonal discount to a customer based on current season
     */
    @PostMapping("/apply/{customerId}")
    public ResponseEntity<?> applySeasonalDiscount(
            @PathVariable Long customerId,
            @RequestBody Map<String, Map<String, Object>> discounts) {

        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (!customerOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                put("error", "Customer not found");
            }});
        }

        Customer customer = customerOpt.get();
        String currentSeason = getCurrentSeason();
        Map<String, Object> seasonData = discounts.getOrDefault(currentSeason, new HashMap<>());
        java.math.BigDecimal discountPercent = new java.math.BigDecimal(
            ((Number) seasonData.getOrDefault("discountPercent", 0)).doubleValue()
        );

        customer.setDiscountPercentage(discountPercent);
        customerRepository.save(customer);

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("message", "Seasonal discount applied successfully");
            put("customerId", customerId);
            put("season", currentSeason);
            put("discountApplied", discountPercent);
        }});
    }

    /**
     * Get current season
     */
    @GetMapping("/current-season")
    public ResponseEntity<?> getCurrentSeasonInfo() {
        String season = getCurrentSeason();
        Map<String, Object> seasonData = seasonalDiscounts.getOrDefault(season, new HashMap<>());
        
        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("season", season);
            put("discountPercent", seasonData.getOrDefault("discountPercent", 0));
            put("startMonth", seasonData.getOrDefault("startMonth", 0));
            put("startDay", seasonData.getOrDefault("startDay", 0));
            put("endMonth", seasonData.getOrDefault("endMonth", 0));
            put("endDay", seasonData.getOrDefault("endDay", 0));
        }});
    }

    /**
     * Apply seasonal discount to all customers
     */
    @PostMapping("/apply-all")
    public ResponseEntity<?> applySeasonalDiscountToAll(@RequestBody Map<String, Map<String, Object>> discounts) {
        String currentSeason = getCurrentSeason();
        Map<String, Object> seasonData = discounts.getOrDefault(currentSeason, new HashMap<>());
        java.math.BigDecimal discountPercent = new java.math.BigDecimal(
            ((Number) seasonData.getOrDefault("discountPercent", 0)).doubleValue()
        );

        List<Customer> allCustomers = customerRepository.findAll();
        final int totalCustomers = allCustomers.size();

        for (Customer customer : allCustomers) {
            customer.setDiscountPercentage(discountPercent);
            customerRepository.save(customer);
        }

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("message", "Seasonal discount applied to all customers");
            put("season", currentSeason);
            put("discountApplied", discountPercent);
            put("customersUpdated", totalCustomers);
        }});
    }

    /**
     * Determine current season based on today's date
     */
    private static String getCurrentSeason() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1; // 1-12
        int day = cal.get(Calendar.DAY_OF_MONTH);

        if ((month == 3 && day >= 21) || (month > 3 && month < 6) || (month == 6 && day <= 20)) {
            return "SPRING";
        } else if ((month == 6 && day >= 21) || (month > 6 && month < 9) || (month == 9 && day <= 22)) {
            return "SUMMER";
        } else if ((month == 9 && day >= 23) || (month > 9 && month < 12) || (month == 12 && day <= 20)) {
            return "FALL";
        } else {
            return "WINTER";
        }
    }
}
