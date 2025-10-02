package com.rdp.repository;

import com.rdp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCodeIgnoreCase(String productCode);
    boolean existsByBarcode(String barcode);
    Optional<Product> findByProductCodeIgnoreCase(String productCode);

    @Query("""
   select p from Product p
   where lower(p.name) like :like
      or lower(p.genericName) like :like
      or lower(p.productCode) like :like
    """)
    List<Product> searchLike(@Param("like") String like);
}