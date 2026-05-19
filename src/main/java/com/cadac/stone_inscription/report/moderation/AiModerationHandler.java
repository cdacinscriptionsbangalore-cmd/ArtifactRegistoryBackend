package com.cadac.stone_inscription.report.moderation;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AiModerationHandler extends ModerationHandler {

    private static final Logger log = LoggerFactory.getLogger(AiModerationHandler.class);
    private static final String AI_ACTOR = "AI_MODERATOR";
    private static final double AUTO_RESOLVE_THRESHOLD = 0.85;
    private static final int CONNECT_TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();
    private static final int READ_TIMEOUT_MS = (int) Duration.ofSeconds(10).toMillis();
    private static final List<String> RESPONSE_WRAPPER_FIELDS = List.of(
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

    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.moderation.webhook-url}")
    private String aiModerationWebhookUrl;

    @Override
    public void handle(ModerationExecutionContext context) {
        ModerationReport report = context.getReport();
        if (report.getStatus() != ReportStatus.PENDING) {
            passToNext(context);
            return;
        }

        report.transitionTo(ReportStatus.AI_SCREENING, AI_ACTOR, ModerationAction.NONE, context.getNote());

        Double score = computeConfidenceScore(context);
        if (score == null) {
            return;
        }

        report.setAiConfidenceScore(score, AI_ACTOR);

        if (score >= AUTO_RESOLVE_THRESHOLD) {
            ModerationAction action = determineAutoAction(report.getReason());
            context.getReportActionService().applyAction(report, context.getTarget(), action, AI_ACTOR, context.getNote());
            report.transitionTo(ReportStatus.RESOLVED, AI_ACTOR, action, context.getNote());
            return;
        }

        report.transitionTo(ReportStatus.ESCALATED, AI_ACTOR, ModerationAction.ESCALATE, context.getNote());
    }

    private Double computeConfidenceScore(ModerationExecutionContext context) {
        ModerationReport report = context.getReport();
        if (!isWebhookConfigured()) {
            log.warn("AI moderation webhook URL is not configured for report {}", report.getId());
            return null;
        }

        Map<String, Object> requestBody = buildWebhookRequestBody(report, context.getTarget());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String rawResponse = invokeWebhook(requestBody, headers);
            return extractConfidenceScore(rawResponse);
        } catch (RestClientException ex) {
            log.warn("AI moderation webhook call failed for report {}: {}",
                    report.getId(), ex.getMessage());
            return null;
        }
    }

    protected String invokeWebhook(Map<String, Object> requestBody, HttpHeaders headers) {
        return restTemplate.postForObject(
                aiModerationWebhookUrl,
                new HttpEntity<>(requestBody, headers),
                String.class);
    }

    protected boolean isWebhookConfigured() {
        return !isBlank(aiModerationWebhookUrl);
    }

    private Map<String, Object> buildWebhookRequestBody(ModerationReport report, ResolvedReportTarget target) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        String content = target == null ? null : target.getContent();
        String details = report.getDetails();
        String combinedReportReason = buildReportReason(report.getReason(), details);

        requestBody.put("title", extractTitle(target));
        requestBody.put("topic", extractTopic(target));
        requestBody.put("description", content);
        requestBody.put("source", isBlank(details) ? content : details);
        requestBody.put("report_reason", combinedReportReason);

        // Keep the original fields for compatibility with any downstream consumers.
        requestBody.put("reportId", report.getId() == null ? null : report.getId().toHexString());
        requestBody.put("reason", report.getReason() == null ? null : report.getReason().name());
        requestBody.put("content", content);
        requestBody.put("details", details);

        return requestBody;
    }

    private String extractTitle(ResolvedReportTarget target) {
        if (target == null || target.getEntity() == null) {
            return fallbackTitle(target);
        }

        Object entity = target.getEntity();
        if (entity instanceof InscriptionPost post) {
            String title = post.getDescription() == null ? null : post.getDescription().getTitle();
            return isBlank(title) ? fallbackTitle(target) : title;
        }

        if (entity instanceof User user) {
            return isBlank(user.getName()) ? fallbackTitle(target) : user.getName();
        }

        return fallbackTitle(target);
    }

    private String extractTopic(ResolvedReportTarget target) {
        if (target == null || target.getEntity() == null) {
            return fallbackTopic(target);
        }

        Object entity = target.getEntity();
        if (entity instanceof InscriptionPost post) {
            if (!isBlank(post.getTopic())) {
                return post.getTopic();
            }

            String subject = post.getDescription() == null ? null : post.getDescription().getSubject();
            return isBlank(subject) ? fallbackTopic(target) : subject;
        }

        if (entity instanceof PublicPostDescription) {
            return "comment";
        }

        if (entity instanceof User) {
            return "user";
        }

        return fallbackTopic(target);
    }

    private String fallbackTitle(ResolvedReportTarget target) {
        ReportTargetType targetType = target == null ? null : target.getType();
        if (targetType == ReportTargetType.COMMENT) {
            return "Reported comment";
        }

        if (targetType == ReportTargetType.USER) {
            return "Reported user";
        }

        return "Reported content";
    }

    private String fallbackTopic(ResolvedReportTarget target) {
        ReportTargetType targetType = target == null ? null : target.getType();
        if (targetType == null) {
            return "report";
        }

        return targetType.name().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildReportReason(ReportReason reason, String details) {
        String reasonValue = reason == null ? null : reason.name();
        if (isBlank(reasonValue)) {
            return details;
        }

        if (isBlank(details)) {
            return reasonValue;
        }

        return reasonValue + " - " + details.trim();
    }

    private ModerationAction determineAutoAction(ReportReason reason) {
        return switch (reason) {
            case SPAM, EXPLICIT_CONTENT, HARASSMENT, HATE_SPEECH, MISINFORMATION -> ModerationAction.REMOVE_CONTENT;
            case OTHER -> ModerationAction.ESCALATE;
        };
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(requestFactory);
    }

    protected Double extractConfidenceScore(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("AI moderation webhook returned blank response");
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            AiModerationResponse response = extractModerationResponse(root);
            if (response == null || response.getConfidenceScore() == null) {
                log.warn("AI moderation response missing confidence field: {}", rawResponse);
                return null;
            }

            return response.getConfidenceScore();
        } catch (IOException ex) {
            log.warn("Failed to parse AI moderation response: {}", rawResponse, ex);
            return null;
        }
    }

    private AiModerationResponse extractModerationResponse(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                AiModerationResponse response = extractModerationResponse(child);
                if (response != null) {
                    return response;
                }
            }
            return null;
        }

        if (!node.isObject()) {
            return null;
        }

        List<AiModerationResponse> wrappedResponses = extractWrappedResponses(node);
        if (!wrappedResponses.isEmpty()) {
            return wrappedResponses.get(0);
        }

        if (looksLikeModerationNode(node)) {
            return objectMapper.convertValue(node, AiModerationResponse.class);
        }

        return null;
    }

    private List<AiModerationResponse> extractWrappedResponses(JsonNode node) {
        List<AiModerationResponse> responses = new ArrayList<>();

        for (String field : RESPONSE_WRAPPER_FIELDS) {
            JsonNode wrappedNode = node.get(field);
            if (wrappedNode != null) {
                AiModerationResponse response = extractModerationResponse(wrappedNode);
                if (response != null) {
                    responses.add(response);
                }
            }
        }

        if (!responses.isEmpty()) {
            return responses;
        }

        for (Map.Entry<String, JsonNode> entry : iterable(node.fields())) {
            if (RESPONSE_WRAPPER_FIELDS.contains(entry.getKey())) {
                continue;
            }

            AiModerationResponse response = extractModerationResponse(entry.getValue());
            if (response != null) {
                responses.add(response);
                return responses;
            }
        }

        return responses;
    }

    private boolean looksLikeModerationNode(JsonNode node) {
        return hasAny(node,
                "confidence", "score", "confidenceScore", "confidence_score", "probability",
                "decision", "verdict", "action",
                "status", "state",
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

    private <T> Iterable<T> iterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AiModerationResponse {
        @JsonProperty("confidence")
        @JsonAlias({ "score", "confidenceScore", "confidence_score", "probability" })
        private Double confidenceScore;

        public Double getConfidenceScore() {
            return confidenceScore;
        }

        public void setConfidenceScore(Double confidenceScore) {
            this.confidenceScore = confidenceScore;
        }
    }
}
