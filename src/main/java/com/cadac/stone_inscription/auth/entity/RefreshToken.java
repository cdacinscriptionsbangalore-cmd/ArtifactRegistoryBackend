package com.cadac.stone_inscription.auth.entity;
import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Document(collection = "t_Refresh_Token")
@Builder
@Data
public class RefreshToken {
    
    @Id
    private String id;
    private String tokenHash;
    private ObjectId userId;
    private String familyId;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUseAt;
    private LocalDateTime createdAt;
    private Boolean revoked;
    private LocalDateTime revokedAt;
    private String sessionRole;
    // private String deviceId;
    // private String ipAddress;
    // private String userAgent;

}
