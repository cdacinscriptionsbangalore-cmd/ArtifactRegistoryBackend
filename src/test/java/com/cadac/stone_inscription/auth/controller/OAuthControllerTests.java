package com.cadac.stone_inscription.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.cadac.stone_inscription.auth.service.StoneAuthService;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTests {

    @InjectMocks
    private OAuthController controller;

    @Mock
    private StoneAuthService stoneAuthService;

    @Test
    void refreshTokenRejectsUnknownOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://evil.example");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.setField(controller, "allowedOrigin", "https://inscriptions.cdacb.in");

        ResponseEntity<?> result = controller.refreshToken(response, request);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void refreshTokenAllowsConfiguredOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://inscriptions.cdacb.in");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.setField(controller, "allowedOrigin", "https://inscriptions.cdacb.in");
        when(stoneAuthService.refreshToken(request, response)).thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = controller.refreshToken(response, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
