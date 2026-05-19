package com.cadac.stone_inscription.auth.controller;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cadac.stone_inscription.admin.service.AdminAccessService;
import com.cadac.stone_inscription.auth.OAuthFlowCookieService;
import com.cadac.stone_inscription.auth.OAuthFlowType;
import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.auth.service.StoneAuthService;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.nimbusds.jose.JOSEException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/oauth2")

public class OAuthController {

    @Autowired
    StoneAuthService stoneAuthService;

    @Autowired
    private OAuthFlowCookieService oAuthFlowCookieService;

    @Autowired
    private AdminAccessService adminAccessService;

    @Value("${app.oauth2.default-provider:google}")
    private String defaultProvider;


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

    @GetMapping("/login/{provider}")
    public void loginWithProvider(
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.USER_LOGIN);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        loginWithProvider(defaultProvider, response);
    }

    @GetMapping("/admin/register/{provider}")
    public void adminRegister(
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.ADMIN_REGISTER);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/admin/register")
    public void adminRegisterDefault(HttpServletResponse response) throws IOException {
        adminRegister(defaultProvider, response);
    }

    @GetMapping("/admin/login/{provider}")
    public void adminLogin(
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.ADMIN_LOGIN);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/admin/login")
    public void adminLoginDefault(HttpServletResponse response) throws IOException {
        adminLogin(defaultProvider, response);
    }

    @GetMapping("/admin/approve")
    public void approveAdminRequest(
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        try {
            adminAccessService.approveRequest(token);
            response.sendRedirect(adminAccessService.getApprovalResultRedirectUrl("approved"));
        } catch (StoneInscriptionException ex) {
            response.sendRedirect(adminAccessService.getApprovalResultRedirectUrl("failed"));
        }
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
