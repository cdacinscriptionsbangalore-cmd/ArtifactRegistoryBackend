package com.cadac.stone_inscription.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

import com.cadac.stone_inscription.auth.JwtUtil;
import com.cadac.stone_inscription.auth.StoneInscriptionUserDetailservice;
import com.cadac.stone_inscription.auth.entity.RefreshToken;
import com.cadac.stone_inscription.auth.repository.RefreshTokenRepo;
import com.cadac.stone_inscription.auth.utill.GenrateRefreshToken;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class StoneAuthServiceImpTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepo refreshTokenRepo;

    @Mock
    private StoneInscriptionUserDetailservice userDetailsService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private StoneAuthServiceImp authService;

    private ObjectId userId;
    private String refreshTokenValue;
    private String refreshTokenHash;
    private RefreshToken existingToken;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        userId = new ObjectId();
        refreshTokenValue = "refresh-token-abc";
        refreshTokenHash = GenrateRefreshToken.hashRefreshToken(refreshTokenValue);
        existingToken = RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .userId(userId)
                .familyId("family-1")
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastUseAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .sessionRole("user")
                .build();

        request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", refreshTokenValue));
        response = new MockHttpServletResponse();

        when(refreshTokenRepo.findByTokenHash(refreshTokenHash)).thenReturn(existingToken);
    }

    @Test
    void refreshTokenRotatesAndRevokesPreviousToken() throws Exception {
        when(userRepository.findByAuthId(userId)).thenReturn(new User());
        when(userDetailsService.loadUserByUsername(any())).thenReturn((UserDetails) org.springframework.security.core.userdetails.User.builder()
                .username("email@example.com")
                .password("pass")
                .roles("USER")
                .build());
        when(jwtUtil.generateToken(any(), eq("user"))).thenReturn("new-access-token");

        ResponseEntity<?> result = authService.refreshToken(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<?, ?> responseBody = (Map<?, ?>) result.getBody();
        assertNotNull(responseBody);
        assertEquals("new-access-token", ((Map<?, ?>) responseBody.get("data")).get("accessToken"));
        assertNotNull(response.getHeader(HttpHeaders.SET_COOKIE));
        assertEquals(true, response.getHeader(HttpHeaders.SET_COOKIE).contains("refreshToken="));
        verify(refreshTokenRepo).save(existingToken);
        verify(refreshTokenRepo, times(2)).save(any(RefreshToken.class));
        assertEquals(true, existingToken.getRevoked());
        assertNotNull(existingToken.getRevokedAt());
    }

    @Test
    void refreshTokenReuseRevokesAllSessions() throws Exception {
        existingToken.setRevoked(true);
        when(refreshTokenRepo.findAllByFamilyId("family-1")).thenReturn(List.of(existingToken));

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> authService.refreshToken(request, response));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        verify(refreshTokenRepo).saveAll(List.of(existingToken));
    }

    @Test
    void refreshTokenExpiredMarksTokenRevokedAndReturnsUnauthorized() {
        existingToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> authService.refreshToken(request, response));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals(true, existingToken.getRevoked());
        assertNotNull(existingToken.getRevokedAt());
    }

    @Test
    void logoutRevokesCurrentRefreshToken() {
        ResponseEntity<?> result = authService.logoutAuth(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(true, existingToken.getRevoked());
        assertNotNull(existingToken.getRevokedAt());
        verify(refreshTokenRepo).save(existingToken);
    }
}
