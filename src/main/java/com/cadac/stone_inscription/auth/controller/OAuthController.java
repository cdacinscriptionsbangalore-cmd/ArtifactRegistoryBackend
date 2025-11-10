package com.cadac.stone_inscription.auth.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cadac.stone_inscription.auth.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/oauth2/login")

public class OAuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {

        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/csrf-token")
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {

        try {
            String oldToken = request.getHeader("Authorization").substring(7);
            System.out.println(oldToken);
            String newToken = jwtUtil.refreshToken(oldToken);
            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Invalid or expired token.");
        }
    }

}
