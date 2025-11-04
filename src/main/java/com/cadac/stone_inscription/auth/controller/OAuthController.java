package com.cadac.stone_inscription.auth.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/oauth2/login")

public class OAuthController {

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {

        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/csrf-token")
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }


}
