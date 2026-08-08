package com.rdp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rdp.dto.BestCustomerDTO;
import com.rdp.model.Billing;
import com.rdp.model.Customer;
import com.rdp.repository.BillingRepository;
import com.rdp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinimumCustomersService {

    private final BillingRepository billingRepository;
    private final CustomerRepository customerRepository;

    /**
     * Get minimum/low-activity customers
     * @param startDate - filter start date (nullable)
     * @param endDate - filter end date (nullable)
     * @param maximumTransactions - maximum number of transactions (default: 5)
     * @param sortBy - "revenue", "frequency", or "recent" (default: frequency)
     * @param limit - max number of results
     * @return list of low-activity customers with metrics
     */
    public List<BestCustomerDTO> getMinimumCustomers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer maximumTransactions,
            String sortBy,
            Integer limit) {

        // Get all billings
        List<Billing> billings;
        if (startDate != null && endDate != null) {
            billings = billingRepository.findByBillingDateBetween(startDate, endDate);
        } else {
            billings = billingRepository.findAll();
        }

        log.info("Found {} billings for minimum customers calculation", billings.size());

        // Group by customer and calculate metrics
        Map<Long, CustomerMetrics> customerMetrics = new HashMap<>();

        for (Billing billing : billings) {
            if (billing.getCustomer() == null) continue;

            Long customerId = billing.getCustomer().getCustomerId();
            customerMetrics.putIfAbsent(customerId, new CustomerMetrics());

            CustomerMetrics metrics = customerMetrics.get(customerId);
            metrics.customer = billing.getCustomer();
            metrics.totalRevenue = metrics.totalRevenue.add(billing.getGrandTotal() != null ? billing.getGrandTotal() : BigDecimal.ZERO);
            metrics.transactionCount++;

            // Track last purchase date
            if (metrics.lastPurchaseDate == null || billing.getBillingDate().isAfter(metrics.lastPurchaseDate)) {
                metrics.lastPurchaseDate = billing.getBillingDate();
            }
        }

        // Filter for customers with LOW activity (less than or equal to maximumTransactions)
        int maxTx = maximumTransactions != null ? maximumTransactions : 5;
        
        List<BestCustomerDTO> results = customerMetrics.values().stream()
                .filter(m -> m.transactionCount <= maxTx)
                .map(metrics -> BestCustomerDTO.builder()
                        .customerId(metrics.customer.getCustomerId())
                        .name(metrics.customer.getName())
                        .phone(metrics.customer.getPhone())
                        .email(metrics.customer.getEmail())
                        .address(metrics.customer.getAddress())
                        .title(metrics.customer.getTitle())
                        .totalRevenue(metrics.totalRevenue.setScale(2, BigDecimal.ROUND_HALF_UP))
                        .purchaseFrequency(metrics.transactionCount)
                        .lastPurchaseDate(metrics.lastPurchaseDate)
                        .averageOrderValue(metrics.transactionCount > 0 
                            ? metrics.totalRevenue.divide(
                                BigDecimal.valueOf(metrics.transactionCount), 2, BigDecimal.ROUND_HALF_UP)
                            : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());

        // Sort based on sortBy parameter (default: by frequency ascending - least active first)
        String sort = sortBy != null ? sortBy.toLowerCase() : "frequency";
        switch (sort) {
            case "revenue":
                results.sort(Comparator.comparing(BestCustomerDTO::getTotalRevenue));
                break;
            case "recent":
                results.sort((a, b) -> {
                    LocalDateTime dateA = a.getLastPurchaseDate();
                    LocalDateTime dateB = b.getLastPurchaseDate();
                    if (dateA == null) return -1;  // Nulls first (oldest)
                    if (dateB == null) return 1;
                    return dateA.compareTo(dateB);
                });
                break;
            case "frequency":
            default:
                results.sort(Comparator.comparing(BestCustomerDTO::getPurchaseFrequency));
                break;
        }

        // Add rank
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        // Apply limit
        int finalLimit = limit != null && limit > 0 ? limit : 50;
        return results.stream().limit(finalLimit).collect(Collectors.toList());
    }

    /**
     * Helper class to aggregate metrics
     */
    private static class CustomerMetrics {
        Customer customer;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Integer transactionCount = 0;
        LocalDateTime lastPurchaseDate;
    }
}
