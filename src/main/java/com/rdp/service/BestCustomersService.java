package com.rdp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class BestCustomersService {

    private final BillingRepository billingRepository;
    private final CustomerRepository customerRepository;

    /**
     * Get best customers based on filters
     * @param startDate - filter start date (nullable)
     * @param endDate - filter end date (nullable)
     * @param minimumTransactions - minimum number of transactions
     * @param sortBy - "revenue", "frequency", or "recent" (default: revenue)
     * @param limit - max number of results
     * @return list of best customers with metrics
     */
    public List<BestCustomerDTO> getBestCustomers(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer minimumTransactions,
            String sortBy,
            Integer limit) {

        // Get all billings
        List<Billing> billings;
        if (startDate != null && endDate != null) {
            billings = billingRepository.findByBillingDateBetween(startDate, endDate);
        } else {
            billings = billingRepository.findAll();
        }

        log.info("Found {} billings for best customers calculation", billings.size());

        // Group by customer and calculate metrics
        Map<Long, BestCustomerMetrics> customerMetrics = new HashMap<>();

        for (Billing billing : billings) {
            if (billing.getCustomer() == null) continue;

            Long customerId = billing.getCustomer().getCustomerId();
            customerMetrics.putIfAbsent(customerId, new BestCustomerMetrics());

            BestCustomerMetrics metrics = customerMetrics.get(customerId);
            metrics.customer = billing.getCustomer();
            metrics.totalRevenue = metrics.totalRevenue.add(billing.getGrandTotal() != null ? billing.getGrandTotal() : BigDecimal.ZERO);
            metrics.transactionCount++;

            // Track last purchase date
            if (metrics.lastPurchaseDate == null || billing.getBillingDate().isAfter(metrics.lastPurchaseDate)) {
                metrics.lastPurchaseDate = billing.getBillingDate();
            }
        }

        // Calculate average order value and apply minimum transactions filter
        List<BestCustomerDTO> results = customerMetrics.values().stream()
                .filter(m -> m.transactionCount >= (minimumTransactions != null ? minimumTransactions : 1))
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
                        .averageOrderValue(metrics.totalRevenue.divide(
                                BigDecimal.valueOf(metrics.transactionCount), 2, BigDecimal.ROUND_HALF_UP))
                        .build())
                .collect(Collectors.toList());

        // Sort based on sortBy parameter
        String sort = sortBy != null ? sortBy.toLowerCase() : "revenue";
        switch (sort) {
            case "frequency":
                results.sort((a, b) -> Integer.compare(b.getPurchaseFrequency(), a.getPurchaseFrequency()));
                break;
            case "recent":
                results.sort((a, b) -> {
                    LocalDateTime dateA = a.getLastPurchaseDate();
                    LocalDateTime dateB = b.getLastPurchaseDate();
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                });
                break;
            case "revenue":
            default:
                results.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
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
    private static class BestCustomerMetrics {
        Customer customer;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Integer transactionCount = 0;
        LocalDateTime lastPurchaseDate;
    }
}
