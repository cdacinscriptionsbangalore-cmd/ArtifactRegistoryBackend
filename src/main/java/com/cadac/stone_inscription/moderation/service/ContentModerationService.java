package com.cadac.stone_inscription.moderation.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.moderation.client.N8nModerationClient;
import com.cadac.stone_inscription.moderation.config.ContentModerationProperties;
import com.cadac.stone_inscription.moderation.dto.ContentModerationRequestDto;
import com.cadac.stone_inscription.moderation.dto.ContentModerationResponseDto;
import com.cadac.stone_inscription.moderation.model.ContentModerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class ContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);
    private static final List<String> MODERATION_WRAPPER_FIELDS = List.of(
            "data",
            "result",
            "results",
            "response",
            "responses",
            "body",
            "payload",
            "output",
            "outputs",
            "json");

    private final N8nModerationClient n8nModerationClient;
    private final ContentModerationProperties properties;
    private final ObjectMapper objectMapper;

    public ContentModerationService(N8nModerationClient n8nModerationClient,
            ContentModerationProperties properties) {
        this.n8nModerationClient = n8nModerationClient;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    public ContentModerationResult moderate(String title, String topic, String description) {
        validateRequest(title, topic, description);

        String rawResponse;
        try {
            rawResponse = n8nModerationClient.moderate(ContentModerationRequestDto.builder()
                    .title(title)
                    .topic(topic)
                    .description(description)
                    .build());
        } catch (RestClientResponseException ex) {
            log.error("Content moderation webhook returned error status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new StoneInscriptionException(
                    "Content moderation webhook failed: " + ex.getStatusCode() + " "
                            + safeErrorMessage(ex.getResponseBodyAsString()),
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException ex) {
            log.error("Content moderation webhook call failed: {}", ex.getMessage(), ex);
            throw new StoneInscriptionException(
                    "Content moderation service is unavailable. Your content was not saved. Cause: "
                            + safeErrorMessage(ex.getMessage()),
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        List<ContentModerationResponseDto> response = parseResponse(rawResponse);

        if (response == null || response.isEmpty() || response.get(0) == null) {
            log.error("Content moderation webhook returned empty or invalid response");
            throw new StoneInscriptionException(
                    "Content moderation service returned an invalid response. Your content was not saved.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        ContentModerationResponseDto moderationResponse = response.get(0);
        Double confidence = moderationResponse.getConfidence() == null ? 0.0 : moderationResponse.getConfidence();
        String decision = normalize(moderationResponse.getDecision());
        String status = normalize(moderationResponse.getStatus());
        boolean approved = isApproved(decision, status, confidence);

        return ContentModerationResult.builder()
                .approved(approved)
                .label(normalize(moderationResponse.getLabel()))
                .confidence(confidence)
                .decision(decision)
                .status(status)
                .reason(cleanReason(moderationResponse.getReason()))
                .build();
    }

    public String buildRejectionMessage(ContentModerationResult moderationResult) {
        String reason = moderationResult.getReason();
        if (reason != null && !reason.isBlank()) {
            return "Content failed moderation and was not saved: " + reason;
        }

        return String.format(
                "Content failed moderation and was not saved. decision=%s, status=%s, confidence=%s, threshold=%s",
                fallbackValue(moderationResult.getDecision()),
                fallbackValue(moderationResult.getStatus()),
                moderationResult.getConfidence() == null ? "null" : moderationResult.getConfidence(),
                properties.getSafeThreshold());
    }

    private void validateRequest(String title, String topic, String description) {
        if (topic == null || topic.isBlank()) {
            throw new StoneInscriptionException("Topic is required for content moderation.", HttpStatus.BAD_REQUEST);
        }

        if (description == null || description.isBlank()) {
            throw new StoneInscriptionException("Description is required for content moderation.", HttpStatus.BAD_REQUEST);
        }

        if (title == null) {
            throw new StoneInscriptionException("Title is required for content moderation.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String cleanReason(String reason) {
        if (reason == null) {
            return null;
        }

        return reason.trim();
    }

    private boolean isApproved(String decision, String status, Double confidence) {
        double score = confidence == null ? 0.0 : confidence;
        boolean rejectedDecision = "BLOCK".equals(decision) || "REJECT".equals(decision) || "DENY".equals(decision);
        boolean rejectedStatus = "REJECTED".equals(status) || "BLOCKED".equals(status) || "DENIED".equals(status);
        if (rejectedDecision || rejectedStatus) {
            return false;
        }

        boolean pendingReview = "REVIEW".equals(decision) || "PENDING_REVIEW".equals(status)
                || "UNDER_REVIEW".equals(status);
        if (pendingReview) {
            return true;
        }

        boolean acceptedDecision = "ALLOW".equals(decision) || "APPROVED".equals(decision);
        boolean acceptedStatus = "APPROVED".equals(status) || "ALLOW".equals(status);
        return score >= properties.getSafeThreshold() && (acceptedDecision || acceptedStatus);
    }

    private String fallbackValue(String value) {
        return value == null || value.isBlank() ? "null" : value;
    }

    private List<ContentModerationResponseDto> parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.error("Content moderation webhook returned blank response");
            throw new StoneInscriptionException(
                    "Content moderation service returned an invalid response. Your content was not saved.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            log.info("Content moderation raw response: {}", rawResponse);

            List<ContentModerationResponseDto> extractedResponses = extractModerationResponses(root);
            if (!extractedResponses.isEmpty()) {
                return extractedResponses;
            }
        } catch (IOException ex) {
            log.error("Failed to parse content moderation response body={}", rawResponse, ex);
            throw new StoneInscriptionException(
                    "Content moderation service returned an unreadable response. Your content was not saved.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        log.error("Unsupported content moderation response body={}", rawResponse);
        throw new StoneInscriptionException(
                "Content moderation service returned an unsupported response. Your content was not saved.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    private List<ContentModerationResponseDto> extractModerationResponses(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }

        if (node.isArray()) {
            List<ContentModerationResponseDto> responses = new ArrayList<>();
            for (JsonNode child : node) {
                responses.addAll(extractModerationResponses(child));
            }
            return responses;
        }

        if (!node.isObject()) {
            return List.of();
        }

        List<ContentModerationResponseDto> wrappedResponses = extractWrappedResponses(node);
        if (!wrappedResponses.isEmpty()) {
            return wrappedResponses;
        }

        if (looksLikeModerationNode(node)) {
            return List.of(objectMapper.convertValue(node, ContentModerationResponseDto.class));
        }

        return List.of();
    }

    private List<ContentModerationResponseDto> extractWrappedResponses(JsonNode node) {
        List<ContentModerationResponseDto> responses = new ArrayList<>();

        for (String field : MODERATION_WRAPPER_FIELDS) {
            JsonNode wrappedNode = node.get(field);
            if (wrappedNode != null) {
                responses.addAll(extractModerationResponses(wrappedNode));
            }
        }

        if (!responses.isEmpty()) {
            return responses;
        }

        for (Map.Entry<String, JsonNode> entry : iterable(node.fields())) {
            if (MODERATION_WRAPPER_FIELDS.contains(entry.getKey())) {
                continue;
            }

            responses.addAll(extractModerationResponses(entry.getValue()));
            if (!responses.isEmpty()) {
                return responses;
            }
        }

        return responses;
    }

    private boolean looksLikeModerationNode(JsonNode node) {
        return hasAny(node,
                "decision", "verdict", "action",
                "status", "state",
                "confidence", "score", "confidenceScore", "confidence_score", "probability",
                "label", "category", "classification");
    }

    private boolean hasAny(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return true;
            }
        }

        return false;
    }

    private <T> Iterable<T> iterable(java.util.Iterator<T> iterator) {
        return () -> iterator;
    }

    private String safeErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown error";
        }

        return message.replaceAll("\\s+", " ").trim();
    }
}
