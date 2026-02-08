package com.cadac.stone_inscription.auth.entity;
import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Document(collection = "t_Refresh_Token")
@Builder
@Data
public class RefreshToken {

    private String tokenHash;
    private ObjectId userId;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUseAt;
    private LocalDateTime createdAt;
    private Boolean revoke;
    // private String deviceId;
    // private String ipAddress;
    // private String userAgent;

}