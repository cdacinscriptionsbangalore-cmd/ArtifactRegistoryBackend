package com.cadac.stone_inscription.auth.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.auth.StoneInscriptionUserDetailservice;
import com.cadac.stone_inscription.auth.entity.RefreshToken;
import com.cadac.stone_inscription.auth.repository.RefreshTokenRepo;
import com.cadac.stone_inscription.auth.utill.GenrateRefreshToken;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.util.UserResponse;
import com.nimbusds.jose.JOSEException;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class StoneAuthServiceImp implements StoneAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private StoneInscriptionUserDetailservice userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public ResponseEntity<?> logoutAuth(HttpServletRequest request, HttpServletResponse response) {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            Arrays.stream(cookies)
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .ifPresent(token -> {
                        String hash = GenrateRefreshToken.hashRefreshToken(token);
                        RefreshToken rt = refreshTokenRepo.findByTokenHash(hash);
                        if (rt != null && !rt.getRevoke()) {
                            rt.setRevoke(true);
                            refreshTokenRepo.save(rt);
                        }
                    });
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true) // true in prod
                .sameSite("None") // None in prod
                // .secure(false) // true in prod
                // .sameSite("Lax") // None in prod
                .path("/") // TODO: IMPORTANT (see below) .path("/authenticated/refresh-token")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader("Set-Cookie", deleteCookie.toString());

        return UserResponse.responseHandler(
                "Logged out successfully",
                HttpStatus.OK,
                true);
    }

    @Override
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws UsernameNotFoundException, JOSEException {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new StoneInscriptionException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new StoneInscriptionException("Unauthorized", HttpStatus.UNAUTHORIZED));

        RefreshToken refreshTokenobj = refreshTokenRepo
                .findByTokenHash(GenrateRefreshToken.hashRefreshToken(refreshToken));

        if (refreshTokenobj == null) {
            throw new StoneInscriptionException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }
        // access used token
        if (refreshTokenobj.getRevoke()) {
            throw new StoneInscriptionException("Unauthorize ", HttpStatus.UNAUTHORIZED);
        }

        // Token Expired

        if (refreshTokenobj.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenobj.setRevoke(true);
            refreshTokenRepo.save(refreshTokenobj);

            throw new StoneInscriptionException("Unauthorize ", HttpStatus.UNAUTHORIZED);
        }
        // Exceed Ideal Limits commented because no use of ideal logout
        // if
        // (refreshTokenobj.getLastUseAt().plusMinutes(15).isBefore(LocalDateTime.now()))
        // {
        // refreshTokenobj.setRevoke(true);
        // refreshTokenRepo.save(refreshTokenobj);
        // throw new StoneInscriptionException("Unauthorize ", HttpStatus.UNAUTHORIZED);
        // }
        refreshTokenobj.setRevoke(true);

        refreshTokenRepo.save(refreshTokenobj);

        String refreshTokenRotate = GenrateRefreshToken.doGenrateRefreshToken();

        User user = userRepository.findByAuthId(refreshTokenobj.getUserId());
        String role = refreshTokenobj.getSessionRole();
        if (role == null || role.isBlank()) {
            role = "user";
        }
        String accessToken = jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()), role);

        // Object user = userRepo.findById(refreshTokenobj.getUserId().toString());

        // if (user == null) {

        // user = newsclrepo.findById(refreshTokenobj.getUserId().toString());
        // }

        // List<String> roles = (user instanceof User) ? ((User) user).getRoles()
        // : ((SchoolRegistration) user).getRoles();

        // String accessToken = (user instanceof User)
        // ? jwtUtil.generateToken(userDetailsService.loadUserByUsername(((User)
        // user).getUsername()), roles)
        // :
        // jwtUtil.generateToken(userDetailsService.loadUserByUsername(((SchoolRegistration)
        // user).getUsername()), roles);

        RefreshToken rotationObj = RefreshToken.builder().createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .lastUseAt(LocalDateTime.now()).revoke(false)
                .tokenHash(GenrateRefreshToken.hashRefreshToken(refreshTokenRotate))
                .userId(refreshTokenobj.getUserId())
                .sessionRole(role)
                .build();

        refreshTokenRepo.save(rotationObj);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshTokenRotate)
                .httpOnly(true)
                .secure(true) // true in prod
                .sameSite("None") // None in prod
                // .secure(false) // true in prod
                // .sameSite("Lax") // None in prod
                .path("/") // TODO: IMPORTANT (see below)
                .maxAge(Duration.ofDays(30))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        Map<String, String> resp = new HashMap<>();

        resp.put("accessToken", accessToken);

        return UserResponse.responseHandler("Sucessfully updated", HttpStatus.OK, resp);
    }

    @Override
    public ResponseEntity<?> updateLastActive(String refreshToken) {
        RefreshToken refreshTokenobj = refreshTokenRepo
                .findByTokenHash(GenrateRefreshToken.hashRefreshToken(refreshToken));

        if (refreshTokenobj == null) {
            throw new StoneInscriptionException("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        if (refreshTokenobj.getRevoke()) {
            throw new StoneInscriptionException("Unauthorize access used token", HttpStatus.UNAUTHORIZED);
        }

        if (refreshTokenobj.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenobj.setRevoke(true);
            refreshTokenRepo.save(refreshTokenobj);

            throw new StoneInscriptionException("Unauthorize Token Expired", HttpStatus.UNAUTHORIZED);
        }

        if (refreshTokenobj.getLastUseAt().plusMinutes(15).isBefore(LocalDateTime.now())) {
            refreshTokenobj.setRevoke(true);
            refreshTokenRepo.save(refreshTokenobj);
            throw new StoneInscriptionException("Unauthorize Exceed Ideal Limits", HttpStatus.UNAUTHORIZED);
        }

        refreshTokenobj.setLastUseAt(LocalDateTime.now());

        refreshTokenRepo.save(refreshTokenobj);

        return UserResponse.responseHandler("session active", HttpStatus.NO_CONTENT, true);
    }

}
