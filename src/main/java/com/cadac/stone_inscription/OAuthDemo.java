package com.cadac.stone_inscription;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Authentication", description = "OAuth smoke-test and role-check endpoints.")
public class OAuthDemo {

    @GetMapping("/")
    @Operation(summary = "API smoke test", description = "Returns a minimal success message for connectivity checks.")
    public String getMethodName(@Parameter(description = "Echo parameter placeholder.", example = "ping") @RequestParam String param) {
        return "hello ";
    }

    @Secured( "user" )
    @GetMapping("/fail")
    @Operation(summary = "User role check", description = "Requires a user role and returns a simple role-check message.")
    public String getfail() {
        return "hello fail ";
    }

    @GetMapping("/noauth/check")
    @Operation(summary = "Public auth health check", description = "Confirms that public no-auth routes are reachable.")
    public String noAuth() {
        return "This endpoint does not require authentication.";
    }

    // Direct Facebook login endpoint
    @GetMapping("/auth/facebook")
    @Operation(summary = "Facebook login redirect marker", description = "Legacy endpoint that returns the Facebook OAuth redirect target as text.")
    public String facebookLogin() {
        return "redirect:/oauth2/authorization/facebook";
    }

    @Secured({ "admin" })
    @GetMapping("/auth")
    @Operation(summary = "Admin auth check", description = "Requires an admin role and returns a simple authenticated message.")
    public String auth() {
        return "This endpoint requires authentication.";
    }

    @GetMapping("/auth/token")
    @Operation(summary = "Token success marker", description = "Returns a text message used after successful authentication flows.")
    public String authToken() {
        return "Authentication successful. You can now access secured endpoints.";
    }

    @Secured({ "user", "admin" })
    @GetMapping("/authrole")
    @Operation(summary = "User or admin role check", description = "Requires either user or admin role.")
    public String authRole() {
        return "This endpoint requires authentication with role.";
    }

}
