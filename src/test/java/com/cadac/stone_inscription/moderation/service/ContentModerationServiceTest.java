package com.cadac.stone_inscription.moderation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.moderation.client.N8nModerationClient;
import com.cadac.stone_inscription.moderation.config.ContentModerationProperties;
import com.cadac.stone_inscription.moderation.dto.ContentModerationResponseDto;
import com.cadac.stone_inscription.moderation.model.ContentModerationResult;

class ContentModerationServiceTest {

    @Mock
    private N8nModerationClient n8nModerationClient;

    private ContentModerationService contentModerationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setSafeThreshold(0.7);
        properties.setWebhookUrl("https://example.com/moderation");
        contentModerationService = new ContentModerationService(n8nModerationClient, properties);
    }

    @Test
    void shouldApproveContentWhenDecisionIsAllowAndConfidenceMeetsThreshold() {
        when(n8nModerationClient.moderate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(ContentModerationResponseDto.builder()
                        .decision("ALLOW")
                        .label("SAFE")
                        .confidence(0.92)
                        .status("approved")
                        .reason("safe")
                        .build()));

        ContentModerationResult result = contentModerationService.moderate("Title", "History", "Safe content");

        assertTrue(result.isApproved());
        assertEquals("SAFE", result.getLabel());
        assertEquals("ALLOW", result.getDecision());
    }

    @Test
    void shouldRejectContentWhenConfidenceIsBelowThreshold() {
        when(n8nModerationClient.moderate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(ContentModerationResponseDto.builder()
                        .decision("ALLOW")
                        .label("SAFE")
                        .confidence(0.6)
                        .status("approved")
                        .reason("low confidence")
                        .build()));

        ContentModerationResult result = contentModerationService.moderate("Title", "History", "Borderline content");

        assertFalse(result.isApproved());
        assertEquals("SAFE", result.getLabel());
    }

    @Test
    void shouldFailClosedWhenWebhookCallFails() {
        when(n8nModerationClient.moderate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RestClientException("timeout"));

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> contentModerationService.moderate("Title", "History", "Safe content"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }
}
