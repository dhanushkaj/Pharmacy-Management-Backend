package com.rdp.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_targets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"year", "month", "day"})
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SalesTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer day; // 1-31

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;
}
