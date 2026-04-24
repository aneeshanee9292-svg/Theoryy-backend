package com.zym.ecart.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zym.ecart.entity.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
}

