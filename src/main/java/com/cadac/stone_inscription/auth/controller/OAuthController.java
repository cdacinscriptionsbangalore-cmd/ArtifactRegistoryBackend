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
import com.cadac.stone_inscription.api.dto.AccessTokenResponse;
import com.cadac.stone_inscription.api.dto.ApiErrorResponse;
import com.cadac.stone_inscription.api.dto.ApiSuccessResponse;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/oauth2")
@Tag(name = "Authentication", description = "OAuth login redirects, refresh-token rotation, session activity, and logout.")

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
    @Operation(
            summary = "Logout",
            description = "Revokes the refresh token cookie when present and expires the browser cookie.",
            responses = @ApiResponse(responseCode = "200", description = "Logged out",
                    content = @Content(schema = @Schema(implementation = ApiSuccessResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"Logged out successfully\",\"http-status\":\"OK\",\"data\":true}"))))
    public ResponseEntity<?> logoutAuth(HttpServletRequest request, HttpServletResponse response) throws JOSEException {
  
        return stoneAuthService.logoutAuth(request, response);
    }

    @PostMapping("/authenticated/refresh-token")
    @Operation(
            summary = "Refresh access token",
            description = "Rotates the HTTP-only refresh token cookie and returns a new JWT access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed",
                            content = @Content(schema = @Schema(implementation = AccessTokenResponse.class),
                                    examples = @ExampleObject(value = "{\"message\":\"Sucessfully updated\",\"http-status\":\"OK\",\"data\":{\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9...\"}}"))),
                    @ApiResponse(responseCode = "401", description = "Refresh token is missing, revoked, or expired",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<?> refreshToken(HttpServletResponse response, HttpServletRequest request)
            throws UsernameNotFoundException, JOSEException {
       

        return stoneAuthService.refreshToken(request, response);
    }

    @PostMapping("/authenticated/active")
    @Operation(
            summary = "Mark session active",
            description = "Refreshes last-use time for the refresh-token session. Returns no content on success.",
            responses = @ApiResponse(responseCode = "204", description = "Session marked active"))
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
    @Operation(
            summary = "Start user OAuth login",
            description = "Stores the user-login flow marker and redirects to the configured OAuth provider.",
            responses = @ApiResponse(responseCode = "302", description = "Redirect to OAuth provider"))
    public void loginWithProvider(
            @Parameter(description = "OAuth provider registration id.", example = "google")
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.USER_LOGIN);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/login")
    @Operation(summary = "Start default user OAuth login", description = "Redirects to the configured default OAuth provider.")
    public void login(HttpServletResponse response) throws IOException {
        loginWithProvider(defaultProvider, response);
    }

    @GetMapping("/admin/authorization/{provider}")
    public void adminAuth(
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.ADMIN_AUTH);
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/admin/authorization")
    public void adminAuthDefault(
            HttpServletResponse response) throws IOException {
        oAuthFlowCookieService.storeFlow(response, OAuthFlowType.ADMIN_AUTH);
        response.sendRedirect("/oauth2/authorization/" + defaultProvider);
    }

    @GetMapping("/admin/approve")
    @Operation(
            summary = "Approve admin request",
            description = "Consumes an emailed approval token and redirects the browser to the configured approval-result page.",
            responses = @ApiResponse(responseCode = "302", description = "Redirect to approval result page"))
    public void approveAdminRequest(
            @Parameter(description = "Admin approval token.", required = true)
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
