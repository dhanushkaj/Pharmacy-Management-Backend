package com.rdp.repository;

import com.rdp.model.BillingReturnRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingReturnRecordRepository extends JpaRepository<BillingReturnRecord, Long> {
    List<BillingReturnRecord> findByBillingBillingId(Long billingId);
}
