package com.cadac.stone_inscription.auth.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/oauth2/login")

public class OAuthController {

    @GetMapping("/google")
    public void loginWithGoogle(HttpServletResponse response) throws IOException {

        response.sendRedirect("/oauth2/authorization/google");
    }

}
