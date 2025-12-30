package com.rdp.service;

import com.rdp.dto.InventoryReturnRequest;
import com.rdp.dto.InventoryReturnResponse;
import com.rdp.model.InventoryItem;
import com.rdp.model.InventoryReturn;
import com.rdp.model.InventoryReturn.InventoryReturnBuilder;
import com.rdp.model.Product;
import com.rdp.model.Supplier;
import com.rdp.repository.InventoryItemRepository;
import com.rdp.repository.InventoryReturnRepository;
import com.rdp.repository.ProductRepository;
import com.rdp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReturnService {
	// Validates business rules for inventory return
	

	private static final Logger log = LoggerFactory.getLogger(InventoryReturnService.class);

	private final InventoryReturnRepository returnRepo;
	private final ProductRepository productRepo;
	private final SupplierRepository supplierRepo;
	private final InventoryItemRepository inventoryItemRepo;
	private final StockMovementService stockMovementService;

	@Transactional
    public InventoryReturnResponse createReturn(InventoryReturnRequest request) {
        // Find product
        Product product = productRepo.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.productId()));

        // Find supplier if provided
        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = supplierRepo.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));
        }

		// Validate business rules
		validateReturnRequest(request, product);

		// Create inventory return record
		InventoryReturn inventoryReturn = InventoryReturn.builder()
			.product(product)
			.returnType(request.returnType())
			.quantity(request.quantity())
			.unitPrice(request.unitPrice())
			.reason(request.reason())
			.batchNo(request.batchNo())
			.customerName(request.customerName())
			.supplier(supplier)
			.notes(request.notes())
			.build();
		inventoryReturn = returnRepo.save(inventoryReturn);
		// Update product stock based on return type and create bin movements
		updateProductStock(inventoryReturn);
		log.info("Created inventory return: returnId={} productId={} type={} quantity={}",
			inventoryReturn.getReturnId(), product.getProductId(), inventoryReturn.getReturnType(),
			inventoryReturn.getQuantity());
		return mapToResponse(inventoryReturn);
	 }
	private void updateProductStock(InventoryReturn inventoryReturn) {
		Product product = inventoryReturn.getProduct();
		InventoryReturn.ReturnType returnType = inventoryReturn.getReturnType();
		Integer quantity = inventoryReturn.getQuantity();
		java.math.BigDecimal price = inventoryReturn.getUnitPrice();

		java.util.Optional<InventoryItem> optItem = inventoryItemRepo.findByProductProductIdAndPrice(product.getProductId(), price);
		InventoryItem matchItem = optItem.orElse(null);

		if (returnType == InventoryReturn.ReturnType.FROM_CUSTOMER) {
			// Add returned quantity to inventory at the correct price
			if (matchItem != null) {
				int currentStock = matchItem.getStock() != null ? matchItem.getStock() : 0;
				matchItem.setStock(currentStock + quantity);
				inventoryItemRepo.save(matchItem);
				// Create STOCK movement: CUSTOMER_RETURN -> INVENTORY
				com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
				moveReq.setFromBin(com.rdp.model.BinType.CUSTOMER_RETURN);
				moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
				moveReq.setQuantity(quantity);
				moveReq.setReferenceType("INVENTORY_RETURN");
				moveReq.setReferenceId(String.valueOf(inventoryReturn.getReturnId()));
				moveReq.setPerformedBy(inventoryReturn.getCustomerName());
				moveReq.setBatchNo(matchItem.getBatchNo());
				moveReq.setPrice(price);
				stockMovementService.createMovement(product.getProductId(), moveReq);
				log.info("Added stock: productId={} previousStock={} added={} newStock={}", product.getProductId(),
						currentStock, quantity, matchItem.getStock());
			} else {
				// Create new inventory item if none exists at this price
				InventoryItem newItem = InventoryItem.builder().product(product).stock(quantity).price(price).build();
				inventoryItemRepo.save(newItem);
				log.info("Created new inventory item: productId={} price={} addedStock={}", product.getProductId(),
						price, quantity);
				// Create STOCK movement: CUSTOMER_RETURN -> INVENTORY
				com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
				moveReq.setFromBin(com.rdp.model.BinType.CUSTOMER_RETURN);
				moveReq.setToBin(com.rdp.model.BinType.INVENTORY);
				moveReq.setQuantity(quantity);
				moveReq.setReferenceType("INVENTORY_RETURN");
				moveReq.setReferenceId(String.valueOf(inventoryReturn.getReturnId()));
				moveReq.setPerformedBy(inventoryReturn.getCustomerName());
				moveReq.setBatchNo(newItem.getBatchNo());
				moveReq.setPrice(price);
				stockMovementService.createMovement(product.getProductId(), moveReq);
			}
		} else if (returnType == InventoryReturn.ReturnType.TO_SUPPLIER) {
			// Subtract returned quantity from inventory at the correct price
			if (matchItem == null || matchItem.getStock() == null || matchItem.getStock() < quantity) {
				throw new IllegalArgumentException("Insufficient stock at price " + price);
			}
			int currentStock = matchItem.getStock();
			matchItem.setStock(currentStock - quantity);
			inventoryItemRepo.save(matchItem);
			// Create STOCK movement: INVENTORY -> SUPPLIER_RETURN
			com.rdp.dto.CreateMovementRequest moveReq = new com.rdp.dto.CreateMovementRequest();
			moveReq.setFromBin(com.rdp.model.BinType.INVENTORY);
			moveReq.setToBin(com.rdp.model.BinType.SUPPLIER_RETURN);
			moveReq.setQuantity(quantity);
			moveReq.setReferenceType("INVENTORY_RETURN");
			moveReq.setReferenceId(String.valueOf(inventoryReturn.getReturnId()));
			moveReq.setPerformedBy(inventoryReturn.getCreatedBy());
			moveReq.setBatchNo(matchItem.getBatchNo());
			moveReq.setPrice(price);
			stockMovementService.createMovement(product.getProductId(), moveReq);
			log.info("Reduced stock: productId={} previousStock={} removed={} newStock={}", product.getProductId(),
					currentStock, quantity, matchItem.getStock());
		}
		// Optionally handle other return types
	}

	public org.springframework.data.domain.Page<InventoryReturnResponse> getAllReturns(
			org.springframework.data.domain.Pageable pageable) {
		return returnRepo.findAll(pageable).map(this::mapToResponse);
	}

	public org.springframework.data.domain.Page<InventoryReturnResponse> getReturnsByType(
			InventoryReturn.ReturnType returnType, org.springframework.data.domain.Pageable pageable) {
		return returnRepo.findByReturnType(returnType, pageable).map(this::mapToResponse);
	}

	public InventoryReturnResponse getReturnById(Long id) {
		InventoryReturn inventoryReturn = returnRepo.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Inventory return not found: " + id));
		return mapToResponse(inventoryReturn);
	}

	private InventoryReturnResponse mapToResponse(InventoryReturn inventoryReturn) {
		Product product = inventoryReturn.getProduct();
		Supplier supplier = inventoryReturn.getSupplier();

		return new InventoryReturnResponse(inventoryReturn.getReturnId(), product.getProductId(),
				product.getProductCode(), product.getName(), inventoryReturn.getReturnType(),
				inventoryReturn.getQuantity(), inventoryReturn.getUnitPrice(), inventoryReturn.getTotalAmount(),
				inventoryReturn.getReason(), inventoryReturn.getBatchNo(), inventoryReturn.getReturnDate(),
				inventoryReturn.getCustomerName(), supplier != null ? supplier.getSupplierId() : null,
				supplier != null ? supplier.getName() : null, inventoryReturn.getNotes(),
				inventoryReturn.getCreatedAt(), inventoryReturn.getCreatedBy());
	}
	
	
	private void validateReturnRequest(InventoryReturnRequest request, Product product) {
		// For both TO_SUPPLIER and FROM_CUSTOMER, check stock at the specific selling price
		if (request.returnType() == InventoryReturn.ReturnType.TO_SUPPLIER || request.returnType() == InventoryReturn.ReturnType.FROM_CUSTOMER) {
			java.math.BigDecimal price = request.unitPrice();
			if (price == null) {
				throw new IllegalArgumentException("Unit price is required for inventory validation");
			}
			java.util.Optional<InventoryItem> optItem = inventoryItemRepo.findByProductProductIdAndPrice(product.getProductId(), price);
			int stockAtPrice = optItem.map(item -> item.getStock() != null ? item.getStock() : 0).orElse(0);
			if (request.returnType() == InventoryReturn.ReturnType.TO_SUPPLIER && stockAtPrice < request.quantity()) {
				throw new IllegalArgumentException(
						String.format("Insufficient stock at price %.2f. Current: %d, Requested return: %d",
								price, stockAtPrice, request.quantity())
				);
			}
			// For FROM_CUSTOMER, you may want to check if adding is allowed (e.g., max stock), but usually not needed
		}

		// If returning TO_SUPPLIER, supplier should be provided
		if (request.returnType() == InventoryReturn.ReturnType.TO_SUPPLIER && request.supplierId() == null) {
			throw new IllegalArgumentException("Supplier is required when returning to supplier");
		}

		// If returning FROM_CUSTOMER, customer name should be provided
		if (request.returnType() == InventoryReturn.ReturnType.FROM_CUSTOMER &&
				(request.customerName() == null || request.customerName().isBlank())) {
			throw new IllegalArgumentException("Customer name is required when receiving return from customer");
		}
	}

}