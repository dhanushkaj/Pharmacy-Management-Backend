package com.rdp.repository;

import com.rdp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCodeIgnoreCase(String productCode);
    boolean existsByBarcode(String barcode);
    Optional<Product> findByProductCodeIgnoreCase(String productCode);
}