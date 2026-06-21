package com.rdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rdp.model.BillingReturnRecord;

@Repository
public interface BillingReturnRecordRepository extends JpaRepository<BillingReturnRecord, Long> {
    @Query("FROM BillingReturnRecord br WHERE br.billing.billingId = :billingId")
    List<BillingReturnRecord> findByBillingBillingId(@Param("billingId") Long billingId);
}
