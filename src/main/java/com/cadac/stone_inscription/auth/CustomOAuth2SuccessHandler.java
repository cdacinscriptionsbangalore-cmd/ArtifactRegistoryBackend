package com.cadac.stone_inscription.auth;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.admin.service.AdminAccessService;
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

    private static final Logger log =
            LoggerFactory.getLogger(CustomOAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AdminAccessService adminAccessService;
    private final OAuthFlowCookieService oAuthFlowCookieService;

    @Value("${app.frontend.oauth.callback-url}")
    private String frontendCallbackUrl;

    @Value("${app.frontend.oauth.admin-callback-url}")
    private String frontendAdminCallbackUrl;

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
        OAuthFlowType flowType = oAuthFlowCookieService.readFlow(request);
        log.debug("DEBUG flowType: " + flowType);
        log.debug("DEBUG handler: flowType=" + flowType
                + " email=" + email);

        UserAuth userAuth = findOrCreateUser(email, name, picture, provider);
        oAuthFlowCookieService.clearFlow(response);
        if (flowType == OAuthFlowType.ADMIN_AUTH) {
            log.debug("DEBUG ADMIN_AUTH: isApprovedAdmin="
                    + adminAccessService.isApprovedAdmin(email));
            if (adminAccessService.isApprovedAdmin(email)) {
                issueRefreshCookie(response,
                        userAuth.getId(), "admin");
                redirectAdmin(request, response,
                        "success", "admin_login");
                return;
            }
            adminAccessService
                    .createOrRefreshPendingRequest(
                            userAuth, name, provider);
            redirectAdmin(request, response,
                    "pending", "admin_register");
            return;
        }

        issueRefreshCookie(response,
                userAuth.getId(), "user");
        getRedirectStrategy().sendRedirect(
                request, response,
                frontendCallbackUrl + "?status=success");
    }

    private UserAuth findOrCreateUser(String email, String name, String picture, String provider) {
        UserAuth userAuth = userAuthRepository.findByEmail(email);
        if (userAuth != null) {
            return userAuth;
        }

        UserAuth newUser = UserAuth.builder()
                .username(email)
                .email(email)
                .provider(provider)
                .passwordHash("oauth")
                .roles(List.of("user"))
                .build();

        UserAuth savedUserAuth = userAuthRepository.save(newUser);
        ObjectId userId = savedUserAuth.getId();

        User profile = User.builder()
                .username(email)
                .name(name)
                .email(email)
                .profileImage(picture)
                .active(true)
                .authId(userId)
                .build();

        userRepository.save(profile);
        return savedUserAuth;
    }

    private void issueRefreshCookie(HttpServletResponse response, ObjectId userId, String sessionRole) {
        String refreshToken = GenrateRefreshToken.doGenrateRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(GenrateRefreshToken.hashRefreshToken(refreshToken))
                .userId(userId)
                .familyId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .lastUseAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .sessionRole(sessionRole)
                .build();

        refreshTokenRepo.save(refreshTokenEntity);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response,
            String status,
            String flow) throws IOException {
        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendCallbackUrl + "?status=" + status + "&flow=" + flow);
    }

    private void redirectAdmin(
            HttpServletRequest request,
            HttpServletResponse response,
            String status,
            String flow) throws IOException {
        getRedirectStrategy().sendRedirect(
                request,
                response,
                frontendAdminCallbackUrl + "?status=" + status + "&flow=" + flow);
    }
}
