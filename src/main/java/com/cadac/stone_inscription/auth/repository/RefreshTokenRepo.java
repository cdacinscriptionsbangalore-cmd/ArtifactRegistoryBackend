package com.cadac.stone_inscription.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.auth.entity.RefreshToken;

@Repository
public interface RefreshTokenRepo extends MongoRepository<RefreshToken, String> {

    RefreshToken findByTokenHash(String refreshToken);

    java.util.List<RefreshToken> findAllByUserId(org.bson.types.ObjectId userId);

    java.util.List<RefreshToken> findAllByFamilyId(String familyId);

    void deleteByTokenHash(String tokenHash);

    void deleteByExpiresAtBeforeOrRevokedAtBefore(java.time.LocalDateTime expiresAt, java.time.LocalDateTime revokedAt);

}