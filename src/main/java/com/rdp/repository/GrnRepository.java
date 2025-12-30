package com.rdp.repository;

import com.rdp.model.Grn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrnRepository extends JpaRepository<Grn, Long> {
    Optional<Grn> findByGrnCode(String grnCode);
    
    long countByGrnCodeStartingWith(String prefix);
}