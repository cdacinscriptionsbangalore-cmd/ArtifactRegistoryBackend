package com.cadac.stone_inscription.auth.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.auth.entity.RefreshToken;

@Repository
public interface RefreshTokenRepo extends MongoRepository<RefreshToken, String> {

    RefreshToken findByTokenHash(String refreshToken);

    void deleteByTokenHash(String tokenHash);

}
