package com.rdp.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.dto.BillingRequest;
import com.rdp.dto.BillingResponse;
import com.rdp.dto.StockMovementDto;
import com.rdp.service.BillingService;
import com.rdp.service.StockMovementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/billings")

@RequiredArgsConstructor
public class BillingController {
	@GetMapping("/credit-report")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<com.rdp.dto.CustomerCreditReportDto>> getCustomerCreditReport(
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "phone", required = false) String phone,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
		List<com.rdp.dto.CustomerCreditReportDto> report = service.getCustomerCreditReport(name, phone, startDate,
				endDate);
		return ResponseEntity.ok(report);
	}

	@PutMapping("/{billingNumber}/mark-paid")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> markBillAsPaid(@PathVariable String billingNumber) {
		service.markBillAsPaid(billingNumber);
		return ResponseEntity.ok().build();
	}

	private final BillingService service;
	private final StockMovementService stockMovementService;

	// Get all stock movements for a billing
	@GetMapping("/{id}/movements")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<java.util.List<StockMovementDto>> getMovementsForBilling(@PathVariable("id") Long billingId) {
		java.util.List<StockMovementDto> movements = stockMovementService.findByReference("BILL",
				String.valueOf(billingId));
		return ResponseEntity.ok(movements);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteBilling(@PathVariable("id") Long id) {
		service.deleteBilling(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<BillingResponse> createBilling(@Valid @RequestBody BillingRequest request) {
		BillingResponse response = service.createBilling(request);
		return ResponseEntity.created(URI.create("/api/billings/" + response.billingId())).body(response);
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public Page<BillingResponse> getAllBillings(@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return service.getAllBillings(pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public BillingResponse getBillingById(@PathVariable("id") Long id) {
		return service.getBillingById(id);
	}

	@GetMapping("/date-range")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
	public Page<BillingResponse> getBillingsByDateRange(
			@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return service.getBillingsByDateRange(startDate, endDate, pageable);
	}

	@GetMapping("/customer/{customerId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public Page<BillingResponse> getBillingsByCustomer(@PathVariable("customerId") Long customerId,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return service.getBillingsByCustomer(customerId, pageable);
	}

	@PutMapping("/{id}/mark-printed")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public BillingResponse markAsPrinted(@PathVariable("id") Long id) {
		return service.markAsPrinted(id);
	}

	@GetMapping("/by-number/{billingNumber}")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<BillingResponse> getBillingByNumber(@PathVariable("billingNumber") String billingNumber) {
		var billing = service.getBillingByNumber(billingNumber);
		if (billing == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(billing);
	}

	@PostMapping("/return")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<com.rdp.dto.BillingReturnResponse> processReturn(
			@Valid @RequestBody com.rdp.dto.BillingReturnRequest request) {
		com.rdp.dto.BillingReturnResponse response = service.processReturn(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/batch-return")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<com.rdp.dto.BatchBillingReturnResponse> processBatchReturn(
			@Valid @RequestBody com.rdp.dto.BatchBillingReturnRequest request) {
		com.rdp.dto.BatchBillingReturnResponse response = service.processBatchReturn(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/attach-returns")
	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER')")
	public ResponseEntity<BillingResponse> attachReturns(@PathVariable("id") Long billingId,
			@Valid @RequestBody com.rdp.dto.AttachReturnsRequest request) {
		BillingResponse response = service.attachReturns(billingId, request);
		return ResponseEntity.ok(response);
	}
}
