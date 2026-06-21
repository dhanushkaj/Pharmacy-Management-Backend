package com.rdp.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Query("FROM Role r WHERE r.roleName = :roleName")
    Optional<Role> findByRoleName(@Param("roleName") String roleName);
    
    @Query("FROM Role r WHERE r.roleName IN :roleNames")
    Set<Role> findByRoleNameIn(@Param("roleNames") List<String> roleNames);
}
