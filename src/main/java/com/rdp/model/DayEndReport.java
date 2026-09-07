
package com.rdp.model;

import java.util.List;

import com.rdp.audit.BaseAuditableEntity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rdp_day_end_reports", indexes = {
        @Index(name = "rdp_idx_day_end_report_date", columnList = "date", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DayEndReport extends BaseAuditableEntity {
                @Transient
                private double oldManualBillTotal;
                @Transient
                private java.util.List<String> oldManualBillDetails;

                public double getOldManualBillTotal() { return oldManualBillTotal; }
                public void setOldManualBillTotal(double oldManualBillTotal) { this.oldManualBillTotal = oldManualBillTotal; }
                public java.util.List<String> getOldManualBillDetails() { return oldManualBillDetails; }
                public void setOldManualBillDetails(java.util.List<String> oldManualBillDetails) { this.oldManualBillDetails = oldManualBillDetails; }
            @Column(name = "cash_value", nullable = false)
            private double cashValue;
            public double getCashValue() { return cashValue; }
            public void setCashValue(double cashValue) { this.cashValue = cashValue; }
      
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "day_end_report_id")
    private Long id;

    @Column(name = "date", nullable = false, unique = true)
    private String date;

    // Header Info
    private String branch;
    private String posId;
    private String cashier;
    private String shift;
    private String dayEndNo;

    // Denomination breakdown (notes)
    @ElementCollection
    @CollectionTable(name = "rdp_day_end_note_breakdown", joinColumns = @JoinColumn(name = "day_end_report_id"))
    private List<Denomination> noteDenominations;

    // Denomination breakdown (coins)
    @ElementCollection
    @CollectionTable(name = "rdp_day_end_coin_breakdown", joinColumns = @JoinColumn(name = "day_end_report_id"))
    private List<Denomination> coinDenominations;

    // Next day opening float: cash set aside from the drawer for tomorrow (entered separately from Physical Cash Value, netted out of Expected Cash)
    @ElementCollection
    @CollectionTable(name = "rdp_day_end_next_day_float_notes", joinColumns = @JoinColumn(name = "day_end_report_id"))
    private List<Denomination> nextDayFloatNoteDenominations;

    @ElementCollection
    @CollectionTable(name = "rdp_day_end_next_day_float_coins", joinColumns = @JoinColumn(name = "day_end_report_id"))
    private List<Denomination> nextDayFloatCoinDenominations;

    private double nextDayFloatTotal;

    // Not persisted: the previous day's nextDayFloatTotal, surfaced so today's opening balance is known
    @Transient
    private double openingBalanceFromPreviousDay;

    public double getOpeningBalanceFromPreviousDay() { return openingBalanceFromPreviousDay; }
    public void setOpeningBalanceFromPreviousDay(double openingBalanceFromPreviousDay) { this.openingBalanceFromPreviousDay = openingBalanceFromPreviousDay; }

    // Non-cash collections
    // Removed: cardPayments, onlineTransfers, customerChequePayments (auto-filled from billing)

    // Supplier payments (with mode)
    @ElementCollection
    @CollectionTable(name = "rdp_day_end_supplier_payments", joinColumns = @JoinColumn(name = "day_end_report_id"))
    private List<SupplierPayment> supplierPayments;

    // System sales summary
    private double totalSales;
    private double cashSales;
    private double cardSales;
    private double onlineTransferSales;
    private double chequeSales;
    private double returns;

    // Reconciliation
    private double expectedCash;
    private double physicalCashCounted;
    private double difference;
    private String status;
    private String differenceReason;

    // Sign-off
    private String cashierSignature;
    private String supervisorSignature;
    private String printedOn;

    // Getters and setters for all fields (omitted for brevity)

    
    @Transient
    private List<String> creditCustomerDetails;

    @Transient
    private double creditCustomerTotal;

    public double getCreditCustomerTotal() { return creditCustomerTotal; }
    public void setCreditCustomerTotal(double creditCustomerTotal) { this.creditCustomerTotal = creditCustomerTotal; }
    
    
    @Embeddable
    public static class Denomination {
        private int value;
        private int qty;
        private double total;
        public Denomination() {}
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }
        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }
    }

    @Embeddable
    public static class SupplierPayment {
        private String supplierName;
        private String mode; // Cash or Cheque
        private double amount;
        public SupplierPayment() {}
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}
