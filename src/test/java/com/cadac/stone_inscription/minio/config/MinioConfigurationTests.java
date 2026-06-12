package com.cadac.stone_inscription.minio.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MinioConfigurationTests {

    private final MinioConfiguration configuration = new MinioConfiguration();

    @Test
    void acceptsDockerServiceEndpointWithoutScheme() {
        MinioProperties properties = validProperties();
        properties.setEndpoint("minio:9000");

        assertNotNull(configuration.minioClient(properties));
    }

    @Test
    void rejectsMissingCredentials() {
        MinioProperties properties = validProperties();
        properties.setSecretKey("");

        assertThrows(IllegalStateException.class, () -> configuration.minioClient(properties));
    }

    private MinioProperties validProperties() {
        MinioProperties properties = new MinioProperties();
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("minio-access-key");
        properties.setSecretKey("minio-secret-key");
        properties.setBucket("stone-inscription");
        return properties;
    }
}
