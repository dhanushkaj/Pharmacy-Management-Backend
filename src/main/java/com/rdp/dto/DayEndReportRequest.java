package com.rdp.dto;

import java.util.List;

public class DayEndReportRequest {
    private String branch;
    private String posId;
    private String cashier;
    private String shift;
    private String dayEndNo;
    private List<Denomination> noteDenominations;
    private List<Denomination> coinDenominations;
    // Removed: cardPayments, onlineTransfers, customerChequePayments (auto-filled from billing)
    private List<SupplierPayment> supplierPayments;
    private double totalSales;
    private double cashSales;
    private double cardSales;
    private double onlineTransferSales;
    private double chequeSales;
    private double returns;
    private double expectedCash;
    private double physicalCashCounted;
    private double difference;
    private String status;
    private String differenceReason;
    private String cashierSignature;
    private String supervisorSignature;
    private String printedOn;
    // Added: cashValue field for day-end report
    private double cashValue;

    // Getters and setters for all fields
    public double getCashValue() { return cashValue; }
    public void setCashValue(double cashValue) { this.cashValue = cashValue; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getPosId() { return posId; }
    public void setPosId(String posId) { this.posId = posId; }
    public String getCashier() { return cashier; }
    public void setCashier(String cashier) { this.cashier = cashier; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getDayEndNo() { return dayEndNo; }
    public void setDayEndNo(String dayEndNo) { this.dayEndNo = dayEndNo; }
    public List<Denomination> getNoteDenominations() { return noteDenominations; }
    public void setNoteDenominations(List<Denomination> noteDenominations) { this.noteDenominations = noteDenominations; }
    public List<Denomination> getCoinDenominations() { return coinDenominations; }
    public void setCoinDenominations(List<Denomination> coinDenominations) { this.coinDenominations = coinDenominations; }
    // Removed: cardPayments, onlineTransfers, customerChequePayments getters/setters
    public List<SupplierPayment> getSupplierPayments() { return supplierPayments; }
    public void setSupplierPayments(List<SupplierPayment> supplierPayments) { this.supplierPayments = supplierPayments; }
    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }
    public double getCashSales() { return cashSales; }
    public void setCashSales(double cashSales) { this.cashSales = cashSales; }
    public double getCardSales() { return cardSales; }
    public void setCardSales(double cardSales) { this.cardSales = cardSales; }
    public double getOnlineTransferSales() { return onlineTransferSales; }
    public void setOnlineTransferSales(double onlineTransferSales) { this.onlineTransferSales = onlineTransferSales; }
    public double getChequeSales() { return chequeSales; }
    public void setChequeSales(double chequeSales) { this.chequeSales = chequeSales; }
    public double getReturns() { return returns; }
    public void setReturns(double returns) { this.returns = returns; }
    public double getExpectedCash() { return expectedCash; }
    public void setExpectedCash(double expectedCash) { this.expectedCash = expectedCash; }
    public double getPhysicalCashCounted() { return physicalCashCounted; }
    public void setPhysicalCashCounted(double physicalCashCounted) { this.physicalCashCounted = physicalCashCounted; }
    public double getDifference() { return difference; }
    public void setDifference(double difference) { this.difference = difference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDifferenceReason() { return differenceReason; }
    public void setDifferenceReason(String differenceReason) { this.differenceReason = differenceReason; }
    public String getCashierSignature() { return cashierSignature; }
    public void setCashierSignature(String cashierSignature) { this.cashierSignature = cashierSignature; }
    public String getSupervisorSignature() { return supervisorSignature; }
    public void setSupervisorSignature(String supervisorSignature) { this.supervisorSignature = supervisorSignature; }
    public String getPrintedOn() { return printedOn; }
    public void setPrintedOn(String printedOn) { this.printedOn = printedOn; }

    public static class Denomination {
        private int value;
        private int qty;
        private double total;
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }

    public static class SupplierPayment {
        private String supplierName;
        private String mode; // Cash or Cheque
        private double amount;
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}
