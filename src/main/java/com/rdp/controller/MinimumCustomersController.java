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
import com.rdp.service.MinimumCustomersService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers/minimum")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class MinimumCustomersController {

    private final MinimumCustomersService minimumCustomersService;

    /**
     * Get minimum/low-activity customers with filtering options
     * @param startDate - Filter start date (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param endDate - Filter end date (format: yyyy-MM-dd'T'HH:mm:ss)
     * @param maximumTransactions - Maximum number of transactions (default: 5)
     * @param sortBy - Sort by: frequency (default), revenue, or recent
     * @param limit - Maximum number of results (default: 50)
     * @return List of low-activity customers with metrics
     */
    @GetMapping
    public ResponseEntity<List<BestCustomerDTO>> getMinimumCustomers(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "5")
            Integer maximumTransactions,
            @RequestParam(required = false, defaultValue = "frequency")
            String sortBy,
            @RequestParam(required = false, defaultValue = "50")
            Integer limit) {

        List<BestCustomerDTO> minimumCustomers = minimumCustomersService.getMinimumCustomers(
                startDate, endDate, maximumTransactions, sortBy, limit);

        return ResponseEntity.ok(minimumCustomers);
    }
}
