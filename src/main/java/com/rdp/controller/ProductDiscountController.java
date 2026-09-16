package com.rdp.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.model.Product;
import com.rdp.repository.ProductRepository;

@RestController
@RequestMapping("/api/products")
public class ProductDiscountController {

  @Autowired
  private ProductRepository productRepository;

  @PostMapping("/{productId}/discount")
  public ResponseEntity<?> applySeasonalDiscount(
      @PathVariable Long productId,
      @RequestBody Map<String, Object> request) {
    
    try {
      Optional<Product> productOpt = productRepository.findById(productId);
      
      if (!productOpt.isPresent()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
      }

      Product product = productOpt.get();
      BigDecimal discountPercent = new BigDecimal(request.get("discountPercentage").toString());
      String startDateStr = (String) request.get("startDate");
      String endDateStr = (String) request.get("endDate");
      LocalDate startDate = LocalDate.parse(startDateStr);
      LocalDate endDate = LocalDate.parse(endDateStr);

      // Check if product already has a discount and return info if it does
      if (product.getMaxDiscount() != null && product.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
        return ResponseEntity.ok(Map.of(
          "hadExistingDiscount", true,
          "previousDiscount", product.getMaxDiscount(),
          "message", "Product already has a discount. Overwriting..."
        ));
      }

      // Update the product with new discount and dates
      product.setMaxDiscount(discountPercent);
      product.setDiscountStartDate(startDate);
      product.setDiscountEndDate(endDate);
      productRepository.save(product);

      return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Seasonal discount saved successfully",
        "productName", product.getName(),
        "discountPercent", discountPercent,
        "startDate", startDate,
        "endDate", endDate
      ));

    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/{productId}/discount/overwrite")
  public ResponseEntity<?> overwriteSeasonalDiscount(
      @PathVariable Long productId,
      @RequestBody Map<String, Object> request) {
    
    try {
      Optional<Product> productOpt = productRepository.findById(productId);
      
      if (!productOpt.isPresent()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
      }

      Product product = productOpt.get();
      BigDecimal discountPercent = new BigDecimal(request.get("discountPercentage").toString());
      String startDateStr = (String) request.get("startDate");
      String endDateStr = (String) request.get("endDate");
      LocalDate startDate = LocalDate.parse(startDateStr);
      LocalDate endDate = LocalDate.parse(endDateStr);

      // Overwrite the discount and dates
      product.setMaxDiscount(discountPercent);
      product.setDiscountStartDate(startDate);
      product.setDiscountEndDate(endDate);
      productRepository.save(product);

      return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Discount overwritten successfully",
        "productName", product.getName(),
        "discountPercent", discountPercent,
        "startDate", startDate,
        "endDate", endDate
      ));

    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
