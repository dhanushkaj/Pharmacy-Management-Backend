package com.rdp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rdp.model.User;


public interface UserRepository extends JpaRepository<User, Long> {
    @Query("FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
}
