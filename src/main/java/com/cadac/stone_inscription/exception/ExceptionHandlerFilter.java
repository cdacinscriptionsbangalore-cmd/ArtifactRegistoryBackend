package com.cadac.stone_inscription.exception;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception handling filter that catches exceptions from other filters
 * and formats them consistently. This runs BEFORE JWT filter.
 */
@Component
public class ExceptionHandlerFilter  extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            handleFilterException(response, ex);
        }
    }

    private void handleFilterException(HttpServletResponse response, Exception ex) throws IOException {
        response.setStatus(determineHttpStatus(ex));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResp = new HashMap<>();
        errorResp.put("error_message", determineErrorMessage(ex));
        errorResp.put("http_status", determineHttpStatusName(ex));
        errorResp.put("http_status_code", determineHttpStatus(ex));

        String jsonResponse = objectMapper.writeValueAsString(errorResp);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private int determineHttpStatus(Exception ex) {
        if (ex instanceof org.springframework.security.authentication.BadCredentialsException ||
            ex instanceof org.springframework.security.core.AuthenticationException) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return HttpStatus.FORBIDDEN.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String determineHttpStatusName(Exception ex) {
        if (ex instanceof org.springframework.security.authentication.BadCredentialsException ||
            ex instanceof org.springframework.security.core.AuthenticationException) {
            return HttpStatus.UNAUTHORIZED.getReasonPhrase();
        }
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return HttpStatus.FORBIDDEN.getReasonPhrase();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
    }

    private String determineErrorMessage(Exception ex) {
        if (ex instanceof org.springframework.security.authentication.BadCredentialsException) {
            return "Invalid JWT token or credentials";
        }
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return "Access denied";
        }
        return "Authentication error occurred";
    }
}