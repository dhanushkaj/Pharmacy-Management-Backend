package com.rdp.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.dto.BestCustomerDTO;
import com.rdp.service.BestCustomersService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers/best")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class BestCustomersController {

    private final BestCustomersService bestCustomersService;

    /**
     * Get best customers with filtering options
     * @param startDate - Filter start date (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param endDate - Filter end date (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param minimumTransactions - Minimum number of transactions
     * @param sortBy - Sort by: revenue (default), frequency, or recent
     * @param limit - Maximum number of results (default: 50)
     * @return List of best customers with metrics
     */
    @GetMapping
    public ResponseEntity<List<BestCustomerDTO>> getBestCustomers(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "1")
            Integer minimumTransactions,
            @RequestParam(required = false, defaultValue = "revenue")
            String sortBy,
            @RequestParam(required = false, defaultValue = "50")
            Integer limit) {

        List<BestCustomerDTO> bestCustomers = bestCustomersService.getBestCustomers(
                startDate, endDate, minimumTransactions, sortBy, limit);

        return ResponseEntity.ok(bestCustomers);
    }
}
