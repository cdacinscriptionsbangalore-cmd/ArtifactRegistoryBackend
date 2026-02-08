package com.cadac.stone_inscription.auth.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.auth.service.StoneAuthService;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.nimbusds.jose.JOSEException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.csrf.CsrfToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/oauth2")

public class OAuthController {

    @Autowired
    StoneAuthService stoneAuthService;


      @PostMapping("/logout")
    public ResponseEntity<?> logoutAuth(HttpServletRequest request, HttpServletResponse response) throws JOSEException {
  
        return stoneAuthService.logoutAuth(request, response);
    }

    @PostMapping("/authenticated/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletResponse response, HttpServletRequest request)
            throws UsernameNotFoundException, JOSEException {
       

        return stoneAuthService.refreshToken(request, response);
    }

    @PostMapping("/authenticated/active")
    public ResponseEntity<?> updateLastActive(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new StoneInscriptionException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        String refreshToken = Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        if (refreshToken == null) {
            throw new StoneInscriptionException("Invalid Request No token found", HttpStatus.BAD_REQUEST);
        }

        return stoneAuthService.updateLastActive(refreshToken);
    }


    // @Autowired
    // private JwtUtil jwtUtil;

    // @GetMapping("/google")
    // public void loginWithGoogle(HttpServletResponse response) throws IOException
    // {

    // response.sendRedirect("/oauth2/authorization/google");
    // }

    // @GetMapping("/csrf-token")
    // public CsrfToken csrfToken(HttpServletRequest request) {
    // return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    // }

    // @PostMapping("/refresh-token")
    // public ResponseEntity<?> refreshToken(HttpServletRequest request) {

    // try {
    // String oldToken = request.getHeader("Authorization").substring(7);
    // System.out.println(oldToken);
    // String newToken = jwtUtil.refreshToken(oldToken);
    // return ResponseEntity.ok(Map.of("token", newToken));
    // } catch (Exception e) {
    // return ResponseEntity.status(403).body("Invalid or expired token.");
    // }
    // }

}
