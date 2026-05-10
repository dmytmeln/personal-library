package org.example.library.security.jwt.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.auth.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository repository;

    @Scheduled(cron = "${application.security.jwt.token-cleanup-cron}")
    public void cleanupExpiredTokens() {
        log.info("Starting expired refresh tokens cleanup job...");
        try {
            repository.purgeExpiredTokens(Instant.now());
            log.info("Successfully cleaned up expired refresh tokens.");
        } catch (Exception e) {
            log.error("Failed to clean up expired refresh tokens: {}", e.getMessage(), e);
        }
    }

}
