package com.cadac.stone_inscription.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cadac.stone_inscription.moderation.client.N8nModerationClient;
import com.cadac.stone_inscription.moderation.config.ContentModerationProperties;
import com.cadac.stone_inscription.moderation.dto.ContentModerationRequestDto;
import com.cadac.stone_inscription.moderation.model.ContentModerationResult;
import com.cadac.stone_inscription.moderation.service.ContentModerationService;

class ContentModerationServiceTests {

    @Test
    void moderateUsesNestedWrappedResponseFromWebhook() {
        ContentModerationService service = new ContentModerationService(new StubModerationClient("""
                {
                  "decision": "REVIEW",
                  "status": "PENDING_REVIEW",
                  "result": {
                    "decision": "ALLOW",
                    "status": "APPROVED",
                    "confidence": 0.94,
                    "label": "safe"
                  }
                }
                """), properties(0.7));

        ContentModerationResult result = service.moderate("Temple Stone", "history", "Ancient inscription details");

        assertTrue(result.isApproved());
        assertEquals("ALLOW", result.getDecision());
        assertEquals("APPROVED", result.getStatus());
        assertEquals(0.94, result.getConfidence());
        assertEquals("SAFE", result.getLabel());
    }

    @Test
    void moderateAllowsPendingReviewResponsesToBeSaved() {
        ContentModerationService service = new ContentModerationService(new StubModerationClient("""
                {
                  "decision": "REVIEW",
                  "status": "pending_review",
                  "confidence": 0,
                  "id": 5006
                }
                """), properties(0.7));

        ContentModerationResult result = service.moderate("Temple Stone", "history", "Normal educational content");

        assertTrue(result.isApproved());
        assertEquals("REVIEW", result.getDecision());
        assertEquals("PENDING_REVIEW", result.getStatus());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    void moderateSupportsCommonAliasFieldsFromWebhook() {
        ContentModerationService service = new ContentModerationService(new StubModerationClient("""
                {
                  "verdict": "approved",
                  "state": "allow",
                  "score": 0.91,
                  "classification": "safe"
                }
                """), properties(0.7));

        ContentModerationResult result = service.moderate("Artifact", "epigraphy", "Clearly valid content");

        assertTrue(result.isApproved());
        assertEquals("APPROVED", result.getDecision());
        assertEquals("ALLOW", result.getStatus());
        assertEquals(0.91, result.getConfidence());
        assertEquals("SAFE", result.getLabel());
    }

    private ContentModerationProperties properties(double threshold) {
        ContentModerationProperties properties = new ContentModerationProperties();
        properties.setSafeThreshold(threshold);
        return properties;
    }

    private static class StubModerationClient extends N8nModerationClient {
        private final String response;

        StubModerationClient(String response) {
            super(new ContentModerationProperties());
            this.response = response;
        }

        @Override
        public String moderate(ContentModerationRequestDto request) {
            return response;
        }
    }
}
