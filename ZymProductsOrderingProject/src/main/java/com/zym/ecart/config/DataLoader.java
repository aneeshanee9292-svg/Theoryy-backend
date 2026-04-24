package com.zym.ecart.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.zym.ecart.service.AdminService;

@Component
public class DataLoader implements CommandLineRunner {
    private final AdminService adminService;

    public DataLoader(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void run(String... args) {
        if (adminService.findByUsername("admin").isEmpty()) {
            //String encodedPassword = passwordEncoder.encode("Admin@123");
            adminService.createAdmin("admin", "Admin@123", "admin@example.com");
        }
    }
}
