package com.cadac.stone_inscription.auth;

import java.time.Duration;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthFlowCookieService {

    private static final Logger log =
            LoggerFactory.getLogger(OAuthFlowCookieService.class);

    public static final String FLOW_COOKIE_NAME = "oauth_flow";

    @Value("${app.cookie.domain}")
    private String cookieDomain;

    public void storeFlow(HttpServletResponse response, OAuthFlowType flowType) {
        ResponseCookie cookie = ResponseCookie.from(FLOW_COOKIE_NAME, flowType.getValue())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .domain(cookieDomain)
                .maxAge(Duration.ofMinutes(10))
                .build();
        log.debug("DEBUG storeFlow: storing flow="
                + flowType.getValue()
                + " domain=" + cookieDomain
                + " cookie=" + cookie.toString());

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public OAuthFlowType readFlow(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        log.debug("DEBUG readFlow: cookies present="
                + (cookies != null ? cookies.length : 0));
        if (cookies == null) {
            return OAuthFlowType.USER_LOGIN;
        }
        log.debug("DEBUG readFlow: oauth_flow cookie value="
                + Arrays.stream(cookies == null ? new Cookie[0] : cookies)
                        .filter(c -> FLOW_COOKIE_NAME.equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse("NOT FOUND"));

        OAuthFlowType flowType = Arrays.stream(cookies)
                .filter(cookie -> FLOW_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .map(OAuthFlowType::fromValue)
                .orElse(OAuthFlowType.USER_LOGIN);
        log.debug("DEBUG readFlow: resolved flowType="
                + flowType);
        return flowType;
    }

    public void clearFlow(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(FLOW_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .domain(cookieDomain)
                .maxAge(Duration.ZERO)
                .build();

        log.debug("DEBUG clearFlow: clearing cookie domain={}", cookieDomain);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
