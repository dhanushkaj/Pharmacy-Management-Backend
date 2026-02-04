package com.rdp.service;

import com.rdp.dto.DayEndReportRequest;
import com.rdp.model.DayEndReport;
import com.rdp.model.Billing;
import com.rdp.repository.DayEndReportRepository;
import com.rdp.repository.BillingRepository;
import com.rdp.repository.InventoryReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DayEndReportService {
    private final DayEndReportRepository dayEndReportRepository;
    private final BillingRepository billingRepository;
    private final InventoryReturnRepository inventoryReturnRepository;

    @Transactional
    public DayEndReport submitDayEndReport(DayEndReportRequest request) {
        // Assume date is today
        String today = LocalDate.now().toString();
        DayEndReport report = dayEndReportRepository.findByDate(today);
        if (report == null) {
            report = new DayEndReport();
            report.setDate(today);
        }
        // Set header fields
        report.setBranch(request.getBranch());
        report.setPosId(request.getPosId());
        report.setCashier(request.getCashier());
        report.setShift(request.getShift());
        report.setDayEndNo(request.getDayEndNo());
        // Set denominations
        // Convert denominations from request DTO to entity type
        if (request.getNoteDenominations() != null) {
            List<DayEndReport.Denomination> noteDenoms = request.getNoteDenominations().stream().map(d -> {
                DayEndReport.Denomination dn = new DayEndReport.Denomination();
                dn.setValue(d.getValue());
                dn.setQty(d.getQty());
                dn.setTotal(d.getTotal());
                return dn;
            }).collect(Collectors.toList());
            report.setNoteDenominations(noteDenoms);
        }
        if (request.getCoinDenominations() != null) {
            List<DayEndReport.Denomination> coinDenoms = request.getCoinDenominations().stream().map(d -> {
                DayEndReport.Denomination dn = new DayEndReport.Denomination();
                dn.setValue(d.getValue());
                dn.setQty(d.getQty());
                dn.setTotal(d.getTotal());
                return dn;
            }).collect(Collectors.toList());
            report.setCoinDenominations(coinDenoms);
        }
        // Set non-cash fields from request
        report.setCardPayments(request.getCardPayments());
        report.setOnlineTransfers(request.getOnlineTransfers());
        report.setCustomerChequePayments(request.getCustomerChequePayments());
        // Supplier payments
        double supplierPaymentsTotal = 0.0;
        if (request.getSupplierPayments() != null) {
            List<DayEndReport.SupplierPayment> supplierPayments = request.getSupplierPayments().stream().map(sp -> {
                DayEndReport.SupplierPayment s = new DayEndReport.SupplierPayment();
                s.setSupplierName(sp.getSupplierName());
                s.setMode(sp.getMode());
                s.setAmount(sp.getAmount());
                return s;
            }).collect(Collectors.toList());
            report.setSupplierPayments(supplierPayments);
            supplierPaymentsTotal = supplierPayments.stream().mapToDouble(DayEndReport.SupplierPayment::getAmount).sum();
        }

        // Calculate system sales summary from billings
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        List<Billing> billings = billingRepository.findByBillingDateBetween(startOfDay, endOfDay);
        double totalSales = 0.0, cashSales = 0.0, cardSales = 0.0, onlineTransferSales = 0.0, chequeSales = 0.0;
        for (Billing b : billings) {
            double amount = b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0;
            totalSales += amount;
            if (b.getPaymentMethod() != null) {
                switch (b.getPaymentMethod()) {
                    case CASH -> cashSales += amount;
                    case CARD -> cardSales += amount;
                    case ONLINE_TRANSFER, MOBILE_PAYMENT -> onlineTransferSales += amount;
                    case CHEQUE -> chequeSales += amount;
                    default -> {}
                }
            }
        }
        report.setTotalSales(totalSales);
        report.setCashSales(cashSales);
        report.setCardSales(cardSales);
        report.setOnlineTransferSales(onlineTransferSales);
        report.setChequeSales(chequeSales);

        // Calculate returns/refunds (customer returns)
        double returns = 0.0;
        List<com.rdp.model.InventoryReturn> customerReturns = inventoryReturnRepository.findCustomerReturnsForDay(startOfDay, endOfDay);
        for (com.rdp.model.InventoryReturn ret : customerReturns) {
            if (ret.getTotalAmount() != null) returns += ret.getTotalAmount().doubleValue();
        }
        report.setReturns(returns);

        // Calculate expected cash: cash sales - returns - supplier payments
        double expectedCash = cashSales - returns - supplierPaymentsTotal;
        report.setExpectedCash(expectedCash);

        // Set physical cash counted from request
        report.setPhysicalCashCounted(request.getPhysicalCashCounted());
        // Calculate difference
        double difference = report.getPhysicalCashCounted() - expectedCash;
        report.setDifference(difference);
        // Set status
        if (Math.abs(difference) < 0.01) {
            report.setStatus("BALANCED");
        } else if (difference < 0) {
            report.setStatus("SHORT");
        } else {
            report.setStatus("EXCESS");
        }
        report.setDifferenceReason(request.getDifferenceReason());
        report.setCashierSignature(request.getCashierSignature());
        report.setSupervisorSignature(request.getSupervisorSignature());
        report.setPrintedOn(request.getPrintedOn());
        return dayEndReportRepository.save(report);
    }

    public DayEndReport getDayEndReportDetails(String date) {
        return dayEndReportRepository.findByDate(date);
    }

    // ...existing code...
}
