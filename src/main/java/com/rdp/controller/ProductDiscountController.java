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

  /**
   * Validates that the requested discount does not exceed the product's configured max discount (ceiling).
   * If no max discount is configured, there is no ceiling to enforce and any value is allowed.
   * Returns an error message if invalid, or null if the discount is allowed.
   */
  private String validateAgainstCeiling(Product product, BigDecimal discountPercent) {
    BigDecimal ceiling = product.getMaxDiscount();
    if (ceiling == null || ceiling.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    if (discountPercent.compareTo(ceiling) > 0) {
      return String.format("Discount cannot exceed the maximum allowed discount of %s%% for this product.", ceiling.stripTrailingZeros().toPlainString());
    }
    return null;
  }

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

      String ceilingError = validateAgainstCeiling(product, discountPercent);
      if (ceilingError != null) {
        return ResponseEntity.badRequest().body(Map.of("error", ceilingError));
      }

      // Check if product already has a seasonal discount and return info if it does
      if (product.getSeasonalDiscountPercent() != null && product.getSeasonalDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
        return ResponseEntity.ok(Map.of(
          "hadExistingDiscount", true,
          "previousDiscount", product.getSeasonalDiscountPercent(),
          "message", "Product already has a discount. Overwriting..."
        ));
      }

      // Update the product with new seasonal discount and dates (maxDiscount stays untouched as the ceiling)
      product.setSeasonalDiscountPercent(discountPercent);
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

      String ceilingError = validateAgainstCeiling(product, discountPercent);
      if (ceilingError != null) {
        return ResponseEntity.badRequest().body(Map.of("error", ceilingError));
      }

      // Overwrite the seasonal discount and dates (maxDiscount stays untouched as the ceiling)
      product.setSeasonalDiscountPercent(discountPercent);
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
