package com.cadac.stone_inscription.auth;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthFlowCookieService {

    public static final String FLOW_COOKIE_NAME = "oauth_flow";

    public void storeFlow(HttpServletResponse response, OAuthFlowType flowType) {
        ResponseCookie cookie = ResponseCookie.from(FLOW_COOKIE_NAME, flowType.getValue())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public OAuthFlowType readFlow(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return OAuthFlowType.USER_LOGIN;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> FLOW_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .map(OAuthFlowType::fromValue)
                .orElse(OAuthFlowType.USER_LOGIN);
    }

    public void clearFlow(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(FLOW_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
