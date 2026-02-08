package com.cadac.stone_inscription.auth.service;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.nimbusds.jose.JOSEException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface StoneAuthService {

    ResponseEntity<?> logoutAuth(HttpServletRequest request, HttpServletResponse response);

    ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) throws UsernameNotFoundException, JOSEException;

    ResponseEntity<?> updateLastActive(String refreshToken);

}
