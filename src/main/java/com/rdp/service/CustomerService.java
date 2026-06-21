package com.rdp.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.CustomerRequest;
import com.rdp.dto.CustomerResponse;
import com.rdp.model.Customer;
import com.rdp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository repo;
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private CustomerResponse toResponse(Customer c) {
        return new CustomerResponse(
                c.getCustomerId(),
                c.getTitle(),
                c.getName(),
                c.getPhone(),
                c.getEmail(),
                c.getAddress(),
                c.getDiscountPercentage(),
                c.getBirthday()
        );
    }

    public List<CustomerResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public CustomerResponse findById(Long id) {
        var c = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer lookup failed id={}", id);
                    return new IllegalArgumentException("Customer not found: " + id);
                });
        log.debug("Customer retrieved id={} name={}", id, c.getName());
        return toResponse(c);
    }

    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        // Uniqueness checks (optional if DB constraints alone are enough)
        if (req.email() != null && !req.email().isBlank() && repo.existsByEmailIgnoreCase(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }
        if (req.phone() != null && !req.phone().isBlank() && repo.existsByPhone(req.phone())) {
            throw new IllegalArgumentException("Phone already exists: " + req.phone());
        }

        var c = new Customer();
        apply(req, c);
        c = repo.save(c);
        log.info("Created customer id={} name={} email={} phone={}", c.getCustomerId(), c.getName(), c.getEmail(), c.getPhone());
        return toResponse(c);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        var c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        if (req.email() != null && !req.email().isBlank()
                && repo.existsByEmailIgnoreCaseAndCustomerIdNot(req.email(), id)) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }
        if (req.phone() != null && !req.phone().isBlank()
                && repo.existsByPhoneAndCustomerIdNot(req.phone(), id)) {
            throw new IllegalArgumentException("Phone already exists: " + req.phone());
        }

        apply(req, c);
        c = repo.save(c);
        log.info("Updated customer id={} name={} email={} phone={}", c.getCustomerId(), c.getName(), c.getEmail(), c.getPhone());
        return toResponse(c);
    }

    @Transactional
    public String delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        repo.deleteById(id);
        log.info("Deleted customer id={}", id);
        return "Customer Deleted " + id;
    }

    private void apply(CustomerRequest req, Customer c) {
        c.setTitle(req.title());
        c.setName(req.name());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setAddress(req.address());
        c.setDiscountPercentage(req.discountPercentage() != null ? req.discountPercentage() : java.math.BigDecimal.ZERO);
        c.setBirthday(req.birthday());
    }
}