package com.zym.ecart.service;

import java.util.Optional;

import com.zym.ecart.entity.Admin;

public interface AdminService {

	public Admin createAdmin(String username, String rawPassword, String email);
	
	public Optional<Admin> findByUsername(String username) ;

}
