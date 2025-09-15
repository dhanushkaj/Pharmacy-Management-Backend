// src/main/java/com/rdp/service/ProductService.java
package com.rdp.service;

import com.rdp.dto.BulkImportResponse;
import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.model.*;
import com.rdp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.validation.Validator;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;

    private static final Pattern PRODUCT_CODE_RE = Pattern.compile("^[A-Za-z]{2}\\d{4}$");


    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getProductId(),
                p.getName(),
                p.getGenericName(),
                p.getCategory() != null ? p.getCategory().getCategoryId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getSupplier() != null ? p.getSupplier().getSupplierId() : null,
                p.getSupplier() != null ? p.getSupplier().getName() : null,
                p.getProductCode(),
                p.getBarcode(),
                p.getCostPrice(),
                p.getPrice(),
                p.getStock(),
                p.getMinStock(),
                p.getMaxStock(),
                p.getMaxDiscount(),
                p.getExpiryDate(),
                p.getPatientInstructions(),
                p.getBinLocation()
        );
    }

    public List<ProductResponse> findAll() {
        return productRepo.findAll().stream().map(this::toResponse).toList();
    }

    public ProductResponse findById(Long id) {
        var p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        return toResponse(p);
    }

    public ProductResponse create(ProductRequest req) {
        var product = new Product();
        apply(req, product);
        return toResponse(productRepo.save(product));
    }

    public ProductResponse update(Long id, ProductRequest req) {
        var product = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        apply(req, product);
        return toResponse(productRepo.save(product));
    }

    public String delete(Long id) {
        if (!productRepo.existsById(id)) throw new IllegalArgumentException("Product not found: " + id);
        productRepo.deleteById(id);
        return "Product Deleted " + id;
    }

    private void apply(ProductRequest req, Product p) {
        p.setName(req.name());
        p.setGenericName(req.genericName());
        p.setProductCode(req.productCode());
        p.setBarcode(req.barcode());
        p.setCostPrice(req.costPrice());
        p.setPrice(req.price());
        p.setStock(req.stock() == null ? 0 : req.stock());
        p.setMinStock(req.minStock());
        p.setMaxStock(req.maxStock());
        p.setMaxDiscount(req.maxDiscount());
        p.setExpiryDate(req.expiryDate());
        p.setPatientInstructions(req.patientInstructions());
        p.setBinLocation(req.binLocation());

        if (req.categoryId() != null) {
            var cat = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
            p.setCategory(cat);
        } else p.setCategory(null);

        if (req.supplierId() != null) {
            var sup = supplierRepo.findById(req.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));
            p.setSupplier(sup);
        } else p.setSupplier(null);
    }

    /** Bulk create: validate each item, continue on errors, return a summary. */
    @Transactional
    public BulkImportResponse bulkCreate(List<ProductRequest> items) {
        int ok = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        // cache existing codes (uppercased) for quick duplicate checks on create
        Set<String> existingCodesUpper = productRepo.findAll().stream()
                .map(Product::getProductCode)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (int i = 0; i < items.size(); i++) {
            int row = i + 2; // assuming row 1 is the Excel header
            ProductRequest req = items.get(i);

            try {
                // Require productCode for both update and create
                if (req.productCode() == null || req.productCode().isBlank()) {
                    throw new IllegalArgumentException("productCode is required");
                }
                String code = req.productCode().toUpperCase();
                if (!PRODUCT_CODE_RE.matcher(code).matches()) {
                    throw new IllegalArgumentException("productCode must match AA9999");
                }

                // Check if a product with this code already exists
                var existingOpt = productRepo.findByProductCodeIgnoreCase(code);

                if (existingOpt.isPresent()) {
                    // ===== UPDATE PATH — increment stock =====
                    var p = existingOpt.get();

                    // If row provides categoryId/supplierId, ensure they exist (don’t change associations here)
                    if (req.categoryId() != null && !categoryRepo.existsById(req.categoryId())) {
                        throw new IllegalArgumentException("Category not found: " + req.categoryId());
                    }
                    if (req.supplierId() != null && !supplierRepo.existsById(req.supplierId())) {
                        throw new IllegalArgumentException("Supplier not found: " + req.supplierId());
                    }

                    int increment = (req.stock() == null ? 0 : req.stock());
                    if (increment < 0) {
                        throw new IllegalArgumentException("Stock to add must be >= 0");
                    }
                    int current = p.getStock() == null ? 0 : p.getStock();
                    p.setStock(current + increment);

                    productRepo.save(p);
                    ok++;
                } else {
                    // ===== CREATE PATH =====
                    // Required fields for new product
                    if (req.name() == null || req.name().isBlank()) {
                        throw new IllegalArgumentException("name is required for new product");
                    }
                    if (req.costPrice() == null) {
                        throw new IllegalArgumentException("costPrice is required for new product");
                    }
                    if (req.price() == null) {
                        throw new IllegalArgumentException("price is required for new product");
                    }

                    // Uniqueness checks
                    if (existingCodesUpper.contains(code) || productRepo.existsByProductCodeIgnoreCase(code)) {
                        throw new IllegalArgumentException("productCode already exists: " + code);
                    }
                    if (req.barcode() != null && !req.barcode().isBlank() && productRepo.existsByBarcode(req.barcode())) {
                        throw new IllegalArgumentException("barcode already exists: " + req.barcode());
                    }

                    // Validate category/supplier if provided
                    if (req.categoryId() != null && !categoryRepo.existsById(req.categoryId())) {
                        throw new IllegalArgumentException("Category not found: " + req.categoryId());
                    }
                    if (req.supplierId() != null && !supplierRepo.existsById(req.supplierId())) {
                        throw new IllegalArgumentException("Supplier not found: " + req.supplierId());
                    }

                    // Normalize request with uppercased productCode, then create
                    var normalized = new ProductRequest(
                            req.name(),
                            req.genericName(),
                            req.categoryId(),
                            req.supplierId(),
                            code,                       // normalized to uppercase
                            req.barcode(),
                            req.costPrice(),
                            req.price(),
                            req.stock(),
                            req.minStock(),
                            req.maxStock(),
                            req.maxDiscount(),
                            req.expiryDate(),
                            req.patientInstructions(),
                            req.binLocation()
                    );

                    create(normalized);
                    existingCodesUpper.add(code);
                    ok++;
                }

            } catch (Exception e) {
                failed++;
                errors.add("Row " + row + ": " + e.getMessage());
            }
        }

        return BulkImportResponse.builder()
                .ok(ok)
                .failed(failed)
                .errors(errors)
                .build();
    }
}
