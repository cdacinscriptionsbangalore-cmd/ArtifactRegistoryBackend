package com.cadac.stone_inscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessTokenResponse", description = "Access token payload returned after refresh-token rotation.")
public class AccessTokenResponse {

    @Schema(description = "New short-lived JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJuser@example.com\"")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
