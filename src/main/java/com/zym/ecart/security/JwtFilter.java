package com.zym.ecart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("🔥 JwtFilter triggered for: " + request.getRequestURI());
        String path = request.getServletPath();

        // ✅ Skip JWT validation for OTP endpoints
        if (path.startsWith("/otp")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 🔍 DEBUG: Print all headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            System.out.println("Header: " + header + " = " + request.getHeader(header));
        }

        try {
            String authHeader = null;

            // ✅ FIX: Read header in case-insensitive way
            Enumeration<String> headers = request.getHeaderNames();
            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                if ("authorization".equalsIgnoreCase(header)) {
                    authHeader = request.getHeader(header);
                    break;
                }
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("❌ Authorization header missing or invalid format");
                filterChain.doFilter(request, response);
                return;
            }
            

            String token = authHeader.substring(7).trim();
            System.out.println("🔑 Token: " + token);

            boolean isValid = jwtUtil.isTokenValid(token);
            System.out.println("🧪 Token valid? " + isValid);

            if (!isValid) {
                filterChain.doFilter(request, response);
                return;
            }

            String subject = jwtUtil.extractSubject(token);
            System.out.println("👤 User: " + subject);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            subject,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println("✅ Authentication set");

        } catch (Exception e) {
            System.out.println("❌ JWT error");
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}