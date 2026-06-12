package com.cadac.stone_inscription.minio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.minio.MinioClient;

@Configuration
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        validate(properties);

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(normalizeEndpoint(properties.getEndpoint()))
                .credentials(properties.getAccessKey(), properties.getSecretKey());

        if (StringUtils.hasText(properties.getRegion())) {
            builder.region(properties.getRegion());
        }

        return builder.build();
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint.contains("://")) {
            return endpoint;
        }

        return "http://" + endpoint;
    }

    private void validate(MinioProperties properties) {
        requireText(properties.getEndpoint(), "storage.minio.endpoint");
        requireText(properties.getAccessKey(), "storage.minio.access-key");
        requireText(properties.getSecretKey(), "storage.minio.secret-key");
        requireText(properties.getBucket(), "storage.minio.bucket");
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
    }
}
