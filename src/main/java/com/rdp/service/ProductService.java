// src/main/java/com/rdp/service/ProductService.java
package com.rdp.service;

import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.model.*;
import com.rdp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;

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
}
