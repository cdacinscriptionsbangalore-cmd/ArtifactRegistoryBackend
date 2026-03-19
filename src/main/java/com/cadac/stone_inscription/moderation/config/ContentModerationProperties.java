package com.cadac.stone_inscription.moderation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "content.moderation")
public class ContentModerationProperties {

    private String webhookUrl;

    private Double safeThreshold = 0.7;

    private Integer connectTimeoutMs = 5000;

    private Integer readTimeoutMs = 10000;

    private Boolean insecureSsl = false;
}
