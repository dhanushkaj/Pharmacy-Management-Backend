package com.rdp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdp.dto.SupplierRequest;
import com.rdp.dto.SupplierResponse;
import com.rdp.model.Supplier;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

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
        var s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        return toResponse(s);
    }

    public SupplierResponse create(SupplierRequest req) {
        var saved = repo.save(Supplier.builder()
                .name(req.name())
                .contactInfo(toJson(req.contact(), req.email(), req.address()))
                .build());
        return toResponse(saved);
    }

    public SupplierResponse update(Long id, SupplierRequest req) {
        var s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
        s.setName(req.name());
        s.setContactInfo(toJson(req.contact(), req.email(), req.address()));
        return toResponse(repo.save(s));
    }

    public String delete(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Supplier not found: " + id);
        repo.deleteById(id);
        return "Supplier Deleted " + id;
    }
}