package com.rdp.service;

import com.rdp.dto.BulkImportResponse;
import com.rdp.dto.ProductRequest;
import com.rdp.dto.ProductResponse;
import com.rdp.model.InventoryItem;
import com.rdp.model.Product;
import com.rdp.model.Supplier;
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
    private final StockMovementService stockMovementService;

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

            if (stock > 0) {
                com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                moveReq.setFromBin(com.rdp.model.BinType.GRN);
                moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                moveReq.setQuantity(stock);
                moveReq.setReferenceType("PRODUCT_CREATE");
                moveReq.setReferenceId(String.valueOf(p.getProductId()));
                moveReq.setPerformedBy("SYSTEM");
                moveReq.setBatchNo(inv.getBatchNo());
                moveReq.setPrice(price);
                stockMovementService.createMovement(p.getProductId(), moveReq);
            }
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

                if (addStock > 0) {
                    com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                    moveReq.setFromBin(com.rdp.model.BinType.GRN);
                    moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                    moveReq.setQuantity(addStock);
                    moveReq.setReferenceType("PRODUCT_UPDATE");
                    moveReq.setReferenceId(String.valueOf(p.getProductId()));
                    moveReq.setPerformedBy("SYSTEM");
                    moveReq.setBatchNo(inv.getBatchNo());
                    moveReq.setPrice(price);                    
                    stockMovementService.createMovement(p.getProductId(), moveReq);
                }
            } else {
                // create new inventory bucket
                InventoryItem inv = InventoryItem.builder()
                        .product(p)
                        
                        .price(price)
                        .costPrice(cost)
                        .stock(addStock)
                        .build();
                inventoryRepo.save(inv);

                if (addStock > 0) {
                    com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                    moveReq.setFromBin(com.rdp.model.BinType.GRN);
                    moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                    moveReq.setQuantity(addStock);
                    moveReq.setReferenceType("PRODUCT_UPDATE");
                    moveReq.setReferenceId(String.valueOf(p.getProductId()));
                    moveReq.setPerformedBy("SYSTEM");
                    moveReq.setBatchNo(inv.getBatchNo());
                    stockMovementService.createMovement(p.getProductId(), moveReq);
                }
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

    /**
     * Improved CSV bulk upload that:
     * - Uses category/supplier names instead of IDs
     * - Auto-generates product codes and barcodes
     * - Updates existing products (by name+genericName+category+supplier)
     * - Rolls back on any error (transactional)
     */
    @Transactional
    public BulkImportResponse bulkCreateFromCsv(List<com.rdp.dto.ProductCsvRequest> items) {
        int ok = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        log.info("Starting CSV bulk product import size={}", items.size());

        // Track used codes in this batch to avoid duplicates
        Set<String> usedCodesInBatch = new HashSet<>();
        Set<String> usedBarcodesInBatch = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            int row = i + 2; // assuming row 1 is header
            com.rdp.dto.ProductCsvRequest req = items.get(i);

            try {
                // 1. Find or validate category
                var category = categoryRepo.findByNameIgnoreCase(req.categoryName())
                        .orElseThrow(() -> new IllegalArgumentException("Category not found: " + req.categoryName()));

                // 2. Find or map supplier (optional)
                Supplier supplier = null;
                if (req.supplierName() != null && !req.supplierName().isBlank()) {
                    supplier = supplierRepo.findByNameIgnoreCase(req.supplierName()).orElse(null);
                    if (supplier == null) {
                        log.warn("Supplier '{}' from CSV row not found; proceeding without supplier for this product", req.supplierName());
                    }
                }

                // 3. Check if product exists (by name + genericName + category + supplier)
                var existingOpt = productRepo.findByNameAndGenericNameAndCategoryAndSupplier(
                        req.name(), req.genericName(), category, supplier);

                Product product;
                if (existingOpt.isPresent()) {
                    // ===== UPDATE PATH =====
                    product = existingOpt.get();
                    log.debug("Updating existing product id={} name={}", product.getProductId(), product.getName());

                    // Update fields (keep existing product code and barcode)
                    if (req.minStock() != null) product.setMinStock(req.minStock());
                    if (req.maxStock() != null) product.setMaxStock(req.maxStock());
                    if (req.maxDiscount() != null) product.setMaxDiscount(req.maxDiscount());
                    if (req.expiryDate() != null) product.setExpiryDate(req.expiryDate());
                    if (req.patientInstructions() != null) product.setPatientInstructions(req.patientInstructions());
                    if (req.binLocation() != null) product.setBinLocation(req.binLocation());

                    product = productRepo.save(product);

                    // Update or create inventory if price/cost/stock provided
                    if (req.price() != null && req.stock() != null && req.stock() > 0) {
                        var invOpt = inventoryRepo.findByProductAndPrice(product, req.price());
                        if (invOpt.isPresent()) {
                            // Add to existing inventory
                            InventoryItem inv = invOpt.get();
                            inv.setStock((inv.getStock() == null ? 0 : inv.getStock()) + req.stock());
                            if (req.costPrice() != null) inv.setCostPrice(req.costPrice());
                            inventoryRepo.save(inv);

                            // Create movement: GRN -> INVENTORY
                            com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                            moveReq.setFromBin(com.rdp.model.BinType.GRN);
                            moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                            moveReq.setQuantity(req.stock());
                            moveReq.setReferenceType("PRODUCT_CSV_UPDATE");
                            moveReq.setReferenceId(String.valueOf(product.getProductId()));
                            moveReq.setPerformedBy("SYSTEM");
                            moveReq.setBatchNo(inv.getBatchNo());
                            stockMovementService.createMovement(product.getProductId(), moveReq);
                        } else {
                            // Create new inventory bucket
                            InventoryItem newInv = InventoryItem.builder()
                                    .product(product)
                                    .price(req.price())
                                    .costPrice(req.costPrice())
                                    .stock(req.stock())
                                    .build();
                            inventoryRepo.save(newInv);

                            // Create movement: GRN -> INVENTORY
                            com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
                            moveReq.setFromBin(com.rdp.model.BinType.GRN);
                            moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
                            moveReq.setQuantity(req.stock());
                            moveReq.setReferenceType("PRODUCT_CSV_CREATE");
                            moveReq.setReferenceId(String.valueOf(product.getProductId()));
                            moveReq.setPerformedBy("SYSTEM");
                            moveReq.setBatchNo(newInv.getBatchNo());
                            stockMovementService.createMovement(product.getProductId(), moveReq);
                        }
                    }
                } else {
                    // ===== CREATE PATH =====
                    product = new Product();
                    product.setName(req.name());
                    product.setGenericName(req.genericName());
                    product.setCategory(category);
                    product.setSupplier(supplier);

                    // Auto-generate product code (AA9999 format)
                    String productCode = generateUniqueProductCode(usedCodesInBatch);
                    product.setProductCode(productCode);
                    usedCodesInBatch.add(productCode.toUpperCase());

                    // Auto-generate barcode (13-digit EAN-13 format)
                    String barcode = generateUniqueBarcode(usedBarcodesInBatch);
                    product.setBarcode(barcode);
                    usedBarcodesInBatch.add(barcode);

                    product.setMinStock(req.minStock());
                    product.setMaxStock(req.maxStock());
                    product.setMaxDiscount(req.maxDiscount());
                    product.setExpiryDate(req.expiryDate());
                    product.setPatientInstructions(req.patientInstructions());
                    product.setBinLocation(req.binLocation());

                    product = productRepo.save(product);
                    log.debug("Created new product id={} code={} name={}", product.getProductId(), product.getProductCode(), product.getName());

                    // Create initial inventory if provided
                    if (req.price() != null && req.stock() != null && req.stock() > 0) {
                        InventoryItem inv = InventoryItem.builder()
                                .product(product)
                                .price(req.price())
                                .costPrice(req.costPrice())
                                .stock(req.stock())
                                .build();
                        inventoryRepo.save(inv);
                    }
                }

                ok++;
            } catch (Exception e) {
                failed++;
                log.warn("CSV bulk import error row={} message={}", row, e.getMessage(), e);
                errors.add("Row " + row + ": " + e.getMessage());
                // Rollback will happen automatically due to @Transactional
                throw new RuntimeException("Bulk import failed at row " + row + ": " + e.getMessage(), e);
            }
        }

        log.info("CSV bulk import completed ok={} failed={}", ok, failed);
        return BulkImportResponse.builder()
                .ok(ok)
                .failed(failed)
                .errors(errors)
                .build();
    }

    /**
     * Generate unique product code in AA9999 format
     */
    private String generateUniqueProductCode(Set<String> usedInBatch) {
        Random random = new Random();
        int attempts = 0;
        while (attempts < 1000) {
            // Generate 2 random letters + 4 random digits
            char letter1 = (char) ('A' + random.nextInt(26));
            char letter2 = (char) ('A' + random.nextInt(26));
            int number = random.nextInt(10000); // 0-9999
            String code = String.format("%c%c%04d", letter1, letter2, number);

            if (!usedInBatch.contains(code.toUpperCase()) && !productRepo.existsByProductCodeIgnoreCase(code)) {
                return code;
            }
            attempts++;
        }
        throw new IllegalStateException("Could not generate unique product code after 1000 attempts");
    }

    /**
     * Generate unique 13-digit barcode (EAN-13 format)
     */
    private String generateUniqueBarcode(Set<String> usedInBatch) {
        Random random = new Random();
        int attempts = 0;
        while (attempts < 1000) {
            // Generate 12 random digits + 1 check digit
            StringBuilder barcode = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                barcode.append(random.nextInt(10));
            }
            // Calculate EAN-13 check digit
            int checkDigit = calculateEAN13CheckDigit(barcode.toString());
            barcode.append(checkDigit);

            String barcodeStr = barcode.toString();
            if (!usedInBatch.contains(barcodeStr) && !productRepo.existsByBarcode(barcodeStr)) {
                return barcodeStr;
            }
            attempts++;
        }
        throw new IllegalStateException("Could not generate unique barcode after 1000 attempts");
    }

    /**
     * Calculate EAN-13 check digit
     */
    private int calculateEAN13CheckDigit(String barcode12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(barcode12.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit;
    }

    public List<ProductResponse> search(String like) {
        return productRepo.searchLike(like).stream().map(this::toResponse).toList();
    }

    // New: search by query string and optional category id
    public List<ProductResponse> search(String q, Long categoryId) {
        String like = "%" + q.trim().toLowerCase() + "%";
        return productRepo.searchLikeAndCategory(like, categoryId).stream().map(this::toResponse).toList();
    }
}
