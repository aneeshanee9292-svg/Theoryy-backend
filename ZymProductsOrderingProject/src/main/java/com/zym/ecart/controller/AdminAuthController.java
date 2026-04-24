package com.zym.ecart.controller;

import com.zym.ecart.dto.LoginRequestDto;
import com.zym.ecart.entity.Admin;
import com.zym.ecart.service.AdminService;
import com.zym.ecart.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/auth")
@CrossOrigin
public class AdminAuthController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDto dto) {
        Optional<Admin> adminOpt = adminService.findByUsername(dto.getUsername());
        System.out.println("Login attempt for: " + dto.getUsername());

        if (adminOpt.isPresent() && passwordEncoder.matches(dto.getPassword(), adminOpt.get().getPassword())) {
            String token = jwtUtil.generateToken(dto.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", token);
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid credentials");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
