package com.cadac.stone_inscription.auth;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.repository.UserAuthRepository;
import com.cadac.stone_inscription.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final StoneInscriptionUserDetailservice userDetailsService;
    private final JwtUtil jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // 1. Check if user exists
        UserAuth existingUser = userAuthRepository.findByEmail(email);

        if (existingUser == null) {
            // 2. If not, create a new user

            UserAuth newUser = UserAuth.builder().email(email).provider(provider).passwordHash("oauth").roles(List.of("user")).build();
            ObjectId objectId = userAuthRepository.save(newUser).getId();

            User profile = User.builder().name(name).profileImage(picture).email(email).active(true).authId(objectId)
                    .build();
            userRepository.save(profile);

            existingUser = newUser;
        }

        ;
        // 3. Generate JWT
        String token = null;
        try {
            token = jwtTokenProvider.generateToken(userDetailsService.loadUserByUsername(email),
                    existingUser.getRoles().get(0));
        } catch (UsernameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JOSEException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String cookieValue = String.format(
                "token=%s; userId=%s; Path=/; Max-Age=%d; Secure; SameSite=none",
                token,
                existingUser.getId().toHexString(),
                24 * 60 * 60 // 24 hours in seconds
        );

        response.setHeader("Set-Cookie", cookieValue);

        // Redirect without token in URL
        response.sendRedirect("http://localhost:3000/feed");
        // response.sendRedirect("http://localhost:5500/callback.html?token=" + token);
        // // 4. Send JWT as JSON response
        // response.setContentType("application/json");
        // response.setCharacterEncoding("UTF-8");
        // Map<String, String> tokenResponse = Map.of("token", token);
        // response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
    }
}