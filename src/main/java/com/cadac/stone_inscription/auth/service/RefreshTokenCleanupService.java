package com.cadac.stone_inscription.auth.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.auth.repository.RefreshTokenRepo;

@Service
public class RefreshTokenCleanupService {

    private final RefreshTokenRepo refreshTokenRepo;

    public RefreshTokenCleanupService(RefreshTokenRepo refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldTokens() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        refreshTokenRepo.deleteByExpiresAtBeforeOrRevokedAtBefore(threshold, threshold);
    }
}
