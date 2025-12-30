package com.rdp.repository;

import com.rdp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmailIgnoreCaseAndCustomerIdNot(String email, Long excludeId);
    boolean existsByPhoneAndCustomerIdNot(String phone, Long excludeId);
}
