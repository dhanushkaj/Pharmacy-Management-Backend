package com.rdp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.dto.DayEndReportRequest;
import com.rdp.model.Billing;
import com.rdp.model.Customer;
import com.rdp.model.DayEndReport;
import com.rdp.model.SupplierPayment;
import com.rdp.repository.BillingRepository;
import com.rdp.repository.DayEndReportRepository;
import com.rdp.repository.InventoryReturnRepository;
import com.rdp.repository.RdpDayEndManualBillRepository;
import com.rdp.repository.SupplierPaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DayEndReportService {
    private final DayEndReportRepository dayEndReportRepository;
    private final BillingRepository billingRepository;
    private final InventoryReturnRepository inventoryReturnRepository;
    private final RdpDayEndManualBillRepository rdpDayEndManualBillRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;

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
        // Next day opening float: cash set aside from the drawer for tomorrow (entered separately from Physical Cash Value)
        if (request.getNextDayFloatNoteDenominations() != null) {
            List<DayEndReport.Denomination> floatNoteDenoms = request.getNextDayFloatNoteDenominations().stream().map(d -> {
                DayEndReport.Denomination dn = new DayEndReport.Denomination();
                dn.setValue(d.getValue());
                dn.setQty(d.getQty());
                dn.setTotal(d.getTotal());
                return dn;
            }).collect(Collectors.toList());
            report.setNextDayFloatNoteDenominations(floatNoteDenoms);
        }
        if (request.getNextDayFloatCoinDenominations() != null) {
            List<DayEndReport.Denomination> floatCoinDenoms = request.getNextDayFloatCoinDenominations().stream().map(d -> {
                DayEndReport.Denomination dn = new DayEndReport.Denomination();
                dn.setValue(d.getValue());
                dn.setQty(d.getQty());
                dn.setTotal(d.getTotal());
                return dn;
            }).collect(Collectors.toList());
            report.setNextDayFloatCoinDenominations(floatCoinDenoms);
        }
        report.setNextDayFloatTotal(request.getNextDayFloatTotal());
        // Set non-cash fields from request
        // Removed: cardPayments, onlineTransfers, customerChequePayments (auto-filled from billing)
        // Supplier payments
        double supplierPaymentsCashTotal = 0.0;
        if (request.getSupplierPayments() != null) {
            List<DayEndReport.SupplierPayment> supplierPayments = request.getSupplierPayments().stream().map(sp -> {
                DayEndReport.SupplierPayment s = new DayEndReport.SupplierPayment();
                s.setSupplierName(sp.getSupplierName());
                s.setMode(sp.getMode());
                s.setAmount(sp.getAmount());
                return s;
            }).collect(Collectors.toList());
            report.setSupplierPayments(supplierPayments);
            supplierPaymentsCashTotal = supplierPayments.stream()
                .filter(sp -> sp.getMode() != null && sp.getMode().equalsIgnoreCase("Cash"))
                .mapToDouble(DayEndReport.SupplierPayment::getAmount).sum();
        }

        // Calculate system sales summary from billings
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        List<Billing> billings = billingRepository.findByBillingDateBetween(startOfDay, endOfDay);
        double totalSales = 0.0, cashSales = 0.0, cardSales = 0.0, onlineTransferSales = 0.0, chequeSales = 0.0;
        for (Billing b : billings) {
            double amount = b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0;
            // Credit sales aren't counted until actually paid off (added back in via Credit Paid Today below)
            if (b.getPaymentMethod() == Billing.PaymentMethod.CREDIT) {
                continue;
            }
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
        report.setCashSales(cashSales);
        report.setCardSales(cardSales);
        report.setOnlineTransferSales(onlineTransferSales);
        report.setChequeSales(chequeSales);

        // Credit sales aren't cash received today; only what was actually paid off today counts here
        List<Billing> creditPaidToday = billingRepository.findCreditBillsPaidBetween(startOfDay, endOfDay);
        double creditPaidTodayTotal = creditPaidToday.stream()
            .mapToDouble(b -> b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0)
            .sum();
        report.setCreditCustomerTotal(creditPaidTodayTotal);
        report.setTotalSales(totalSales + creditPaidTodayTotal);
        report.setCreditCustomerDetails(creditPaidToday.stream().map(b -> {
            Customer c = b.getCustomer();
            return String.format("%s (%s) - Rs.%.2f | Bill#: %s", c != null ? c.getName() : "N/A", c != null ? c.getPhone() : "", b.getGrandTotal(), b.getBillingNumber());
        }).toList());

        // Calculate returns/refunds (customer returns)
        double returns = 0.0;
        List<com.rdp.model.InventoryReturn> customerReturns = inventoryReturnRepository.findCustomerReturnsForDay(startOfDay, endOfDay);
        for (com.rdp.model.InventoryReturn ret : customerReturns) {
            if (ret.getTotalAmount() != null) returns += ret.getTotalAmount().doubleValue();
        }
        report.setReturns(returns);

        // Calculate expected cash: cash sales - returns - supplier payments + manual bills
        double manualBillsTotal = 0.0;
        List<com.rdp.model.RdpDayEndManualBill> manualBills = rdpDayEndManualBillRepository.findByReportDate(today);
        for (com.rdp.model.RdpDayEndManualBill mb : manualBills) {
            if (mb.getAmount() != null) manualBillsTotal += mb.getAmount();
        }
        // Retained float is money kept back for tomorrow, so it isn't part of what's expected to be handed over today
        double expectedCash = cashSales - returns - supplierPaymentsCashTotal + manualBillsTotal - report.getNextDayFloatTotal();
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
        
     // Fetch and set old manual bills (type OLD_MANUAL)
        List<Billing> oldManualBills = billingRepository.findByBillingDateBetween(startOfDay, endOfDay).stream()
            .filter(b -> b.getPaymentMethod() == Billing.PaymentMethod.OLD_MANUAL)
            .toList();
        double oldManualBillTotal = oldManualBills.stream()
            .mapToDouble(b -> b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0)
            .sum();
        report.setOldManualBillTotal(oldManualBillTotal);
        report.setOldManualBillDetails(oldManualBills.stream().map(b -> {
            Customer c = b.getCustomer();
            return String.format("%s (%s) - Rs.%.2f | Bill#: %s", c != null ? c.getName() : "N/A", c != null ? c.getPhone() : "", b.getGrandTotal(), b.getBillingNumber());
        }).toList());
        
        return dayEndReportRepository.save(report);
    }

    public DayEndReport getDayEndReportDetails(String date) {
                    
        DayEndReport report = null;
        try {
            report = dayEndReportRepository.findByDate(date);
            LocalDate localDate = LocalDate.parse(date);
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            // If no report exists for today yet, start from a transient one (cashier's manual entries default to blank)
            if (report == null) {
                report = new DayEndReport();
                report.setDate(date);
            }

            // Sales summary is always recomputed live from billings, even if today's report was already submitted,
            // so later events (e.g. a credit bill paid after submission) are reflected without needing a resubmit
            List<Billing> billings = billingRepository.findByBillingDateBetween(startOfDay, endOfDay);
            double totalSales = 0.0, cashSales = 0.0, cardSales = 0.0, onlineTransferSales = 0.0, chequeSales = 0.0;
            for (Billing b : billings) {
                double amount = b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0;
                // Credit sales aren't counted until actually paid off (added back in via Credit Paid Today below)
                if (b.getPaymentMethod() == Billing.PaymentMethod.CREDIT) {
                    continue;
                }
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
            report.setCashSales(cashSales);
            report.setCardSales(cardSales);
            report.setOnlineTransferSales(onlineTransferSales);
            report.setChequeSales(chequeSales);
            // Returns/refunds
            double returns = 0.0;
            List<com.rdp.model.InventoryReturn> customerReturns = inventoryReturnRepository.findCustomerReturnsForDay(startOfDay, endOfDay);
            for (com.rdp.model.InventoryReturn ret : customerReturns) {
                if (ret.getTotalAmount() != null) returns += ret.getTotalAmount().doubleValue();
            }
            report.setReturns(returns);

            // Opening balance for the day = previous day's retained next-day float (0.00 if no prior report)
            DayEndReport previousDayReport = dayEndReportRepository.findByDate(localDate.minusDays(1).toString());
            report.setOpeningBalanceFromPreviousDay(previousDayReport != null ? previousDayReport.getNextDayFloatTotal() : 0.0);

            // Credit sales aren't cash received today; only what was actually paid off today counts here
            List<Billing> creditPaidToday = billingRepository.findCreditBillsPaidBetween(startOfDay, endOfDay);
            report.setCreditCustomerDetails(creditPaidToday.stream().map(b -> {
                Customer c = b.getCustomer();
                return String.format("%s (%s) - Rs.%.2f | Bill#: %s", c != null ? c.getName() : "N/A", c != null ? c.getPhone() : "", b.getGrandTotal(), b.getBillingNumber());
            }).toList());
            double creditCustomerTotal = creditPaidToday.stream()
                .mapToDouble(b -> b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0)
                .sum();
            report.setCreditCustomerTotal(creditCustomerTotal);
            report.setTotalSales(totalSales + creditCustomerTotal);
            
            
         // Fetch and set old manual bills (type OLD_MANUAL)
            List<Billing> oldManualBills = billingRepository.findByBillingDateBetween(startOfDay, endOfDay).stream()
                .filter(b -> b.getPaymentMethod() == Billing.PaymentMethod.OLD_MANUAL)
                .toList();
            double oldManualBillTotal = oldManualBills.stream()
                .mapToDouble(b -> b.getGrandTotal() != null ? b.getGrandTotal().doubleValue() : 0.0)
                .sum();
            report.setOldManualBillTotal(oldManualBillTotal);
            report.setOldManualBillDetails(oldManualBills.stream().map(b -> {
                Customer c = b.getCustomer();
                return String.format("%s (%s) - Rs.%.2f | Bill#: %s", c != null ? c.getName() : "N/A", c != null ? c.getPhone() : "", b.getGrandTotal(), b.getBillingNumber());
            }).toList());
            
            // Fetch supplier payments for this day and convert to DayEndReport.SupplierPayment
            LocalDate reportDate = LocalDate.parse(date);
            List<SupplierPayment> supplierPaymentsForDay = supplierPaymentRepository.findPaymentsByDateRange(reportDate, reportDate);
            List<DayEndReport.SupplierPayment> mappedSupplierPayments = supplierPaymentsForDay.stream()
                .map(sp -> {
                    DayEndReport.SupplierPayment dayEndSp = new DayEndReport.SupplierPayment();
                    dayEndSp.setSupplierName(sp.getSupplier() != null ? sp.getSupplier().getName() : "Unknown");
                    dayEndSp.setMode(sp.getPaymentMethod() != null ? sp.getPaymentMethod() : "CASH");
                    dayEndSp.setAmount(sp.getPaymentAmount() != null ? sp.getPaymentAmount().doubleValue() : 0.0);
                    return dayEndSp;
                })
                .collect(Collectors.toList());
            report.setSupplierPayments(mappedSupplierPayments);
            
            return report;
        } catch (Exception e) {
            // Log the error and return a meaningful error response
            System.err.println("Error in getDayEndReportDetails: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get day end report details: " + e.getMessage());
        }
    }

    // ...existing code...
}
