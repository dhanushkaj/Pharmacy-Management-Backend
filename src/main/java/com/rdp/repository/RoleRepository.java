package com.rdp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.rdp.model.Role;


import java.util.List;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
    Set<Role> findByRoleNameIn(List<String> roleNames);
}
