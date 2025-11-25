package com.rdp.service;

import com.rdp.dto.BulkImportResponse;
import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.repository.CategoryRepository;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;
    private final InventoryItemRepository inventoryRepo;

    private static final Pattern PRODUCT_CODE_RE = Pattern.compile("^[A-Za-z]{2}\\d{4}$");

    private ProductResponse toResponse(Product p) {
        // Aggregate stock and pick last price (by createdAt desc -> approximate by last InventoryItem)
        var invList = inventoryRepo.findByProduct(p);
        int totalStock = invList.stream().map(i -> i.getStock() == null ? 0 : i.getStock()).reduce(0, Integer::sum);

        // pick lastPrice/cost by most recent inventory entry (createdAt descending)
        BigDecimal lastPrice = null;
        BigDecimal lastCost = null;
        Optional<InventoryItem> last = invList.stream()
                .max(Comparator.comparing(i -> i.getCreatedAt() != null ? i.getCreatedAt() : java.time.LocalDateTime.MIN));
        if (last.isPresent()) {
            lastPrice = last.get().getPrice();
            lastCost = last.get().getCostPrice();
        }

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
                lastPrice,
                lastCost,
                totalStock,
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
        var p = productRepo.findById(id).orElseThrow(() -> {
            log.warn("Product lookup failed id={}", id);
            return new IllegalArgumentException("Product not found: " + id);
        });
        log.debug("Product retrieved id={} code={}", id, p.getProductCode());
        return toResponse(p);
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        validateProductCode(req.productCode());
        var p = new Product();
        applyToProduct(req, p);
        p = productRepo.save(p);
        log.info("Created product id={} code={} name={}", p.getProductId(), p.getProductCode(), p.getName());

        // create initial inventory item if price/stock provided
        if (req.price() != null || req.stock() != null || req.costPrice() != null) {
            BigDecimal price = req.price() == null ? BigDecimal.ZERO : req.price();
            BigDecimal cost = req.costPrice();
            int stock = req.stock() == null ? 0 : req.stock();
            InventoryItem inv = InventoryItem.builder()
                    .product(p)
                    .price(price)
                    .costPrice(cost)
                    .stock(stock)
                    .build();
            inventoryRepo.save(inv);
        }

        return toResponse(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        var p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (req.productCode() != null) validateProductCode(req.productCode());

        applyToProduct(req, p);
        p = productRepo.save(p);
    log.info("Updated product id={} code={} name={}", p.getProductId(), p.getProductCode(), p.getName());

        // If price/stock provided on update, create or update inventory accordingly:
        if (req.price() != null || req.stock() != null) {
            BigDecimal price = req.price() == null ? BigDecimal.ZERO : req.price();
            BigDecimal cost = req.costPrice();
            int addStock = req.stock() == null ? 0 : req.stock();

            // try to find existing inventory with same price
            var invOpt = inventoryRepo.findByProductAndPrice(p, price);
            if (invOpt.isPresent()) {
                InventoryItem inv = invOpt.get();
                inv.setStock((inv.getStock() == null ? 0 : inv.getStock()) + addStock);
                if (cost != null) inv.setCostPrice(cost);
                inventoryRepo.save(inv);
            } else {
                // create new inventory bucket
                InventoryItem inv = InventoryItem.builder()
                        .product(p)
                        .price(price)
                        .costPrice(cost)
                        .stock(addStock)
                        .build();
                inventoryRepo.save(inv);
            }
        }

        return toResponse(p);
    }

    public String delete(Long id) {
        if (!productRepo.existsById(id)) throw new IllegalArgumentException("Product not found: " + id);
        // Optionally: cascade-delete inventory rows first or rely on FK cascade rules
        var invList = inventoryRepo.findByProduct(productRepo.getReferenceById(id));
        inventoryRepo.deleteAll(invList);
        productRepo.deleteById(id);
        log.info("Deleted product id={} removedInventoryItems={}", id, invList.size());
        return "Product Deleted " + id;
    }

    private void applyToProduct(ProductRequest req, Product p) {
        if (req.name() != null) p.setName(req.name());
        if (req.genericName() != null) p.setGenericName(req.genericName());
        if (req.productCode() != null) p.setProductCode(req.productCode().toUpperCase());
        if (req.barcode() != null) p.setBarcode(req.barcode());
        if (req.minStock() != null) p.setMinStock(req.minStock());
        if (req.maxStock() != null) p.setMaxStock(req.maxStock());
        if (req.maxDiscount() != null) p.setMaxDiscount(req.maxDiscount());
        if (req.expiryDate() != null) p.setExpiryDate(req.expiryDate());
        if (req.patientInstructions() != null) p.setPatientInstructions(req.patientInstructions());
        if (req.binLocation() != null) p.setBinLocation(req.binLocation());

        if (req.categoryId() != null) {
            var cat = categoryRepo.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryId()));
            p.setCategory(cat);
        }

        if (req.supplierId() != null) {
            var sup = supplierRepo.findById(req.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + req.supplierId()));
            p.setSupplier(sup);
        }
    }

    private void validateProductCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("productCode is required");
        String up = code.toUpperCase();
        if (!PRODUCT_CODE_RE.matcher(up).matches()) {
            throw new IllegalArgumentException("productCode must match AA9999");
        }
        // uniqueness check on create is handled by caller; for update you may want to ensure that
    }

    /** Bulk create: adapted to inventory model */
    @Transactional
    public BulkImportResponse bulkCreate(List<ProductRequest> items) {
        int ok = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        log.info("Starting bulk product import size={}", items.size());

        // cache existing codes (uppercased) for quick duplicate checks on create
        Set<String> existingCodesUpper = productRepo.findAll().stream()
                .map(Product::getProductCode)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (int i = 0; i < items.size(); i++) {
            int row = i + 2; // assuming row 1 is header
            ProductRequest req = items.get(i);

            try {
                if (req.productCode() == null || req.productCode().isBlank())
                    throw new IllegalArgumentException("productCode is required");
                String code = req.productCode().toUpperCase();
                if (!PRODUCT_CODE_RE.matcher(code).matches())
                    throw new IllegalArgumentException("productCode must match AA9999");

                var existingOpt = productRepo.findByProductCodeIgnoreCase(code);
                if (existingOpt.isPresent()) {
                    // ===== UPDATE PATH - increment inventory =====
                    Product p = existingOpt.get();

                    if (req.categoryId() != null && !categoryRepo.existsById(req.categoryId()))
                        throw new IllegalArgumentException("Category not found: " + req.categoryId());
                    if (req.supplierId() != null && !supplierRepo.existsById(req.supplierId()))
                        throw new IllegalArgumentException("Supplier not found: " + req.supplierId());

                    int addStock = req.stock() == null ? 0 : req.stock();
                    if (addStock < 0) throw new IllegalArgumentException("Stock to add must be >= 0");

                    BigDecimal price = req.price() == null ? BigDecimal.ZERO : req.price();
                    BigDecimal cost = req.costPrice();

                    // find matching inventory row by price
                    var invOpt = inventoryRepo.findByProductAndPrice(p, price);
                    if (invOpt.isPresent()) {
                        InventoryItem inv = invOpt.get();
                        inv.setStock((inv.getStock() == null ? 0 : inv.getStock()) + addStock);
                        if (cost != null) inv.setCostPrice(cost);
                        inventoryRepo.save(inv);
                    } else {
                        InventoryItem newInv = InventoryItem.builder()
                                .product(p)
                                .price(price)
                                .costPrice(cost)
                                .stock(addStock)
                                .build();
                        inventoryRepo.save(newInv);
                    }

                    ok++;
                } else {
                    // ===== CREATE PATH =====
                    if (req.name() == null || req.name().isBlank())
                        throw new IllegalArgumentException("name is required for new product");
                    if (req.price() == null) throw new IllegalArgumentException("price is required for new product");
                    if (req.costPrice() == null) throw new IllegalArgumentException("costPrice is required for new product");

                    if (existingCodesUpper.contains(code) || productRepo.existsByProductCodeIgnoreCase(code))
                        throw new IllegalArgumentException("productCode already exists: " + code);

                    if (req.barcode() != null && !req.barcode().isBlank() && productRepo.existsByBarcode(req.barcode()))
                        throw new IllegalArgumentException("barcode already exists: " + req.barcode());

                    if (req.categoryId() != null && !categoryRepo.existsById(req.categoryId()))
                        throw new IllegalArgumentException("Category not found: " + req.categoryId());
                    if (req.supplierId() != null && !supplierRepo.existsById(req.supplierId()))
                        throw new IllegalArgumentException("Supplier not found: " + req.supplierId());

                    Product p = new Product();
                    p.setName(req.name());
                    p.setGenericName(req.genericName());
                    p.setProductCode(code);
                    p.setBarcode(req.barcode());
                    p.setMinStock(req.minStock());
                    p.setMaxStock(req.maxStock());
                    p.setMaxDiscount(req.maxDiscount());
                    p.setExpiryDate(req.expiryDate());
                    p.setPatientInstructions(req.patientInstructions());
                    p.setBinLocation(req.binLocation());
                    if (req.categoryId() != null) p.setCategory(categoryRepo.getReferenceById(req.categoryId()));
                    if (req.supplierId() != null) p.setSupplier(supplierRepo.getReferenceById(req.supplierId()));

                    p = productRepo.save(p);

                    int stock = req.stock() == null ? 0 : req.stock();
                    InventoryItem inv = InventoryItem.builder()
                            .product(p)
                            .price(req.price())
                            .costPrice(req.costPrice())
                            .stock(stock)
                            .build();
                    inventoryRepo.save(inv);

                    existingCodesUpper.add(code);
                    ok++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Bulk import error row={} message={}", row, e.getMessage());
                errors.add("Row " + row + ": " + e.getMessage());
            }
        }
        log.info("Bulk import completed ok={} failed={}", ok, failed);
        return BulkImportResponse.builder()
                .ok(ok)
                .failed(failed)
                .errors(errors)
                .build();
    }

    public List<ProductResponse> search(String like) {
        return productRepo.searchLike(like).stream().map(this::toResponse).toList();
    }
}
