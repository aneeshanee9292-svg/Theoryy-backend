package com.zym.ecart.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zym.ecart.entity.Admin;
import com.zym.ecart.repository.AdminRepository;
import com.zym.ecart.service.AdminService;

@Service
public class AdminServiceImpl implements AdminService {
	
	    @Autowired
	    private AdminRepository adminRepository;

	    @Autowired
	    private PasswordEncoder passwordEncoder;

	    @Override
	    public Admin createAdmin(String username, String rawPassword, String email) {
	        Admin admin = new Admin();
	        admin.setUsername(username);
	        admin.setPassword(passwordEncoder.encode(rawPassword)); // hash password
	        admin.setEmail(email);
	        return adminRepository.save(admin);
	    }

		@Override
		public Optional<Admin> findByUsername(String username) {
			return adminRepository.findByUsername(username);
		}
		
	    
	    
	}


