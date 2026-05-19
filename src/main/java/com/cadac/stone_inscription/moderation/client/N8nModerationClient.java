package com.cadac.stone_inscription.moderation.client;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.cadac.stone_inscription.moderation.config.ContentModerationProperties;
import com.cadac.stone_inscription.moderation.dto.ContentModerationRequestDto;

@Component
public class N8nModerationClient {

    private final ContentModerationProperties properties;
    private final RestTemplate restTemplate;

    public N8nModerationClient(ContentModerationProperties properties) {
        this.properties = properties;
        this.restTemplate = buildRestTemplate(properties);
    }

    public String moderate(ContentModerationRequestDto request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                properties.getWebhookUrl(),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<String>() {
                }).getBody();
    }

    private RestTemplate buildRestTemplate(ContentModerationProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        if (Boolean.TRUE.equals(properties.getInsecureSsl())) {
            configureInsecureSsl();
        }

        return new RestTemplate(requestFactory);
    }

    private void configureInsecureSsl() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }

                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to configure insecure SSL for content moderation", ex);
        }
    }
}
