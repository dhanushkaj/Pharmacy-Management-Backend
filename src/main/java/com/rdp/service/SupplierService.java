package com.rdp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdp.dto.SupplierRequest;
import com.rdp.dto.SupplierResponse;
import com.rdp.model.Supplier;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private String toJson(String contact, String email, String address) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "contact", contact == null ? "" : contact,
                    "email",   email   == null ? "" : email,
                    "address", address == null ? "" : address
            ));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize contact_info", e);
        }
    }

    @SuppressWarnings("unchecked")
    private SupplierResponse toResponse(Supplier s) {
        String contact = "", email = "", address = "";
        try {
            if (s.getContactInfo() != null && !s.getContactInfo().isBlank()) {
                var map = mapper.readValue(s.getContactInfo(), Map.class);
                contact = String.valueOf(map.getOrDefault("contact", ""));
                email   = String.valueOf(map.getOrDefault("email", ""));
                address = String.valueOf(map.getOrDefault("address", ""));
            }
        } catch (Exception ignored) {}
        return new SupplierResponse(s.getSupplierId(), s.getName(), contact, email, address);
    }

    public List<SupplierResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public SupplierResponse findById(Long id) {
        var s = repo.findById(id).orElseThrow(() -> {
            log.warn("Supplier lookup failed id={}", id);
            return new IllegalArgumentException("Supplier not found: " + id);
        });
        log.debug("Supplier retrieved id={} name={}", id, s.getName());
        return toResponse(s);
    }

    public SupplierResponse create(SupplierRequest req) {
        var saved = repo.save(Supplier.builder()
                .name(req.name())
                .contactInfo(toJson(req.contact(), req.email(), req.address()))
                .build());
        log.info("Created supplier id={} name={}", saved.getSupplierId(), saved.getName());
        return toResponse(saved);
    }

    public SupplierResponse update(Long id, SupplierRequest req) {
        var s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        s.setName(req.name());
        s.setContactInfo(toJson(req.contact(), req.email(), req.address()));
        s = repo.save(s);
        log.info("Updated supplier id={} name={}", s.getSupplierId(), s.getName());
        return toResponse(s);
    }

    public String delete(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Supplier not found: " + id);
        repo.deleteById(id);
        log.info("Deleted supplier id={}", id);
        return "Supplier Deleted " + id;
    }
}