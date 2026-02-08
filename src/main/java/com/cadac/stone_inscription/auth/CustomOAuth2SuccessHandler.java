package com.cadac.stone_inscription.auth;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.auth.entity.RefreshToken;
import com.cadac.stone_inscription.auth.repository.RefreshTokenRepo;
import com.cadac.stone_inscription.auth.utill.GenrateRefreshToken;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.repository.UserAuthRepository;
import com.cadac.stone_inscription.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final RefreshTokenRepo refreshTokenRepo;

    private static final String FRONTEND_CALLBACK_URL =
            "https://inscriptions.cdacb.in/oauth/callback?status=success";

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        Map<String, Object> attributes =
                oauthToken.getPrincipal().getAttributes();

        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        // 1️⃣ Find or create user
        UserAuth userAuth = userAuthRepository.findByEmail(email);
        ObjectId userId;

        if (userAuth == null) {
            UserAuth newUser = UserAuth.builder()
                    .email(email)
                    .provider(provider)
                    .passwordHash("oauth")
                    .roles(List.of("user"))
                    .build();

            userAuth = userAuthRepository.save(newUser);
            userId = userAuth.getId();

            User profile = User.builder()
                    .name(name)
                    .email(email)
                    .profileImage(picture)
                    .active(true)
                    .authId(userId)
                    .build();

            userRepository.save(profile);
        } else {
            userId = userAuth.getId();
        }

        // 2️⃣ Generate refresh token
        String refreshToken = GenrateRefreshToken.doGenrateRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(GenrateRefreshToken.hashRefreshToken(refreshToken))
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .lastUseAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoke(false)
                .build();

        refreshTokenRepo.save(refreshTokenEntity);

        // 3️⃣ Set refresh token in HttpOnly cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)        // MUST be true in production (HTTPS)
                .sameSite("None")    // REQUIRED for OAuth cross-site redirect
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 4️⃣ Redirect to frontend OAuth callback
        getRedirectStrategy().sendRedirect(
                request,
                response,
                FRONTEND_CALLBACK_URL
        );
    }
}
