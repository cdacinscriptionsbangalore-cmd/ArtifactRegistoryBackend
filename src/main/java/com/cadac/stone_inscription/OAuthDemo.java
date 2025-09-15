package com.cadac.stone_inscription;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class OAuthDemo {

    @GetMapping("/")
    public String getMethodName(@RequestParam String param) {
        return "hello ";
    }

    @Secured( "user" )
    @GetMapping("/fail")
    public String getfail() {
        return "hello fail ";
    }

    @GetMapping("/noauth/check")
    public String noAuth() {
        return "This endpoint does not require authentication.";
    }

    // Direct Facebook login endpoint
    @GetMapping("/auth/facebook")
    public String facebookLogin() {
        return "redirect:/oauth2/authorization/facebook";
    }

    @Secured({ "admin" })
    @GetMapping("/auth")
    public String auth() {
        return "This endpoint requires authentication.";
    }

    @GetMapping("/auth/token")
    public String authToken() {
        return "Authentication successful. You can now access secured endpoints.";
    }

    @Secured({ "user", "admin" })
    @GetMapping("/authrole")
    public String authRole() {
        return "This endpoint requires authentication with role.";
    }

}
