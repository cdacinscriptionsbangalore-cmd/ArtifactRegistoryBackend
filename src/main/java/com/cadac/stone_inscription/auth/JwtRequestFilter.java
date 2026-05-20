package com.cadac.stone_inscription.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cadac.stone_inscription.exception.StoneInscriptionException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws jakarta.servlet.ServletException, IOException {
        try {
            // JWT Token is in the form "Bearer token". Remove Bearer word and
            // get only the Token
            try {
                String jwtToken = extractJwtFromRequest(request);

                System.out.println("filter called");

                if (StringUtils.hasText(jwtToken) && jwtUtil.validateToken(jwtToken)) {
                    UserDetails userDetails = new User(jwtUtil.getUsernameFromToken(jwtToken), "",
                            jwtUtil.getRolesFromToken(jwtToken));

                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // After setting the Authentication in the context, we specify
                    // that the current user is authenticated. So it passes the
                    // Spring Security Configurations successfully.
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                } else {
                    filterChain.doFilter(request, response);
                    return;
                }

            } catch (StoneInscriptionException ex) {

                request.setAttribute("exception", ex);

            }

        } catch (Exception ex) {
            request.setAttribute("exception", ex);
           
            throw ex;
        }

        filterChain.doFilter(request, response);
    }

    public String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {

            // JwtBlackList jwtBlackList =
            // jwtBlackListRepo.findByJwtToken(bearerToken.substring(7,
            // bearerToken.length()));

            // if (jwtBlackList == null) {
            return bearerToken.substring(7, bearerToken.length());
            // }

            // else {

            // throw new CMPFOException("User Already Loged Out", HttpStatus.UNAUTHORIZED);

            // }

        } else {
            
            return null;
        }
    }
}
