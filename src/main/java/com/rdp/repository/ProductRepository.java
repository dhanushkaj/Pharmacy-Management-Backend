package com.rdp.repository;

import com.rdp.model.Category;
import com.rdp.model.Product;
import com.rdp.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCodeIgnoreCase(String productCode);
    boolean existsByBarcode(String barcode);
    Optional<Product> findByProductCodeIgnoreCase(String productCode);

    // Find product by name, generic name, category, and supplier (for CSV update/insert logic)
    Optional<Product> findByNameAndGenericNameAndCategoryAndSupplier(
            String name, String genericName, Category category, Supplier supplier);

    @Query("""
       select p from Product p
       where lower(p.name) like :like
          or lower(p.genericName) like :like
          or lower(p.productCode) like :like
    """)
    List<Product> searchLike(@Param("like") String like);

    // quick existence check by category id
    @Query("select case when count(p) > 0 then true else false end from Product p where p.category.categoryId = :catId")
    boolean existsByCategoryId(@Param("catId") Long categoryId);
}
