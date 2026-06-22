package org.example.library.security.jwt;

import org.example.library.auth.domain.RefreshToken;
import org.example.library.auth.repository.RefreshTokenRepository;
import org.example.library.auth.service.RefreshTokenService;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService service;


    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void shouldGenerateNewTokens() {
        var user = saveUser();

        var response = service.generateNewTokens(user);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshTokenId()).isNotNull();
        var savedToken = refreshTokenRepository.findById(response.refreshTokenId())
                .orElseThrow(() -> new AssertionError("Refresh token not found after generation"));
        assertThat(savedToken.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getExpiryDate()).isAfter(Instant.now());
    }

    @Test
    void shouldRefreshToken() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);

        var response = service.refreshToken(initialResponse.refreshToken(), initialResponse.refreshTokenId());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshTokenId()).isNotNull();
        assertThat(response.refreshTokenId()).isNotEqualTo(initialResponse.refreshTokenId());
        var oldToken = refreshTokenRepository.findById(initialResponse.refreshTokenId())
                .orElseThrow(() -> new AssertionError("Old refresh token not found"));
        assertThat(oldToken.isRevoked())
                .as("Old token should be revoked")
                .isTrue();
        var newToken = refreshTokenRepository.findById(response.refreshTokenId())
                .orElseThrow(() -> new AssertionError("New refresh token not found"));
        assertThat(newToken.isRevoked()).isFalse();
    }

    @Test
    void shouldThrowBadCredentialsWhenTokenIdNotFound() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);

        assertThatThrownBy(() -> service.refreshToken(initialResponse.refreshToken(), -1))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unknown token");
    }

    @Test
    void shouldThrowBadCredentialsWhenTokenHashDoesNotMatch() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);

        assertThatThrownBy(() -> service.refreshToken("invalid-token", initialResponse.refreshTokenId()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void shouldThrowBadCredentialsWhenTokenIsExpired() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);
        var token = refreshTokenRepository.findById(initialResponse.refreshTokenId())
                .orElseThrow(() -> new AssertionError("Refresh token not found"));
        token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        refreshTokenRepository.save(token);

        assertThatThrownBy(() -> service.refreshToken(initialResponse.refreshToken(), initialResponse.refreshTokenId()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void shouldThrowSecurityExceptionAndRevokeWhenEmailMismatch() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);
        user.setEmail("new-email@test.com");
        userRepository.save(user);

        assertThatThrownBy(() -> service.refreshToken(initialResponse.refreshToken(), initialResponse.refreshTokenId()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Identity mismatch. Please log in again.");

        var token = refreshTokenRepository.findById(initialResponse.refreshTokenId())
                .orElseThrow(() -> new AssertionError("Refresh token not found"));
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void shouldThrowSecurityExceptionAndRevokeAllWhenTokenReused() {
        var user = saveUser();
        var initialResponse = service.generateNewTokens(user);
        var secondTokenResponse = service.generateNewTokens(user);
        
        var token = refreshTokenRepository.findById(initialResponse.refreshTokenId())
                .orElseThrow(() -> new AssertionError("Refresh token not found"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        assertThatThrownBy(() -> service.refreshToken(initialResponse.refreshToken(), initialResponse.refreshTokenId()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Breach detected: Refresh token reused.");

        var tokens = refreshTokenRepository.findAll();
        assertThat(tokens).as("All tokens for the user should be revoked").allMatch(RefreshToken::isRevoked);
    }

    @Test
    void shouldIssueTokensOnEmailUpdate() {
        var user = saveUser();
        service.generateNewTokens(user);
        service.generateNewTokens(user);

        var response = service.issueTokensOnEmailUpdate(user);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        var allTokens = refreshTokenRepository.findAll();
        assertThat(allTokens).filteredOn(RefreshToken::isRevoked).hasSize(2);
        assertThat(allTokens).filteredOn(t -> !t.isRevoked())
                .hasSize(1)
                .first()
                .extracting(RefreshToken::getId)
                .isEqualTo(response.refreshTokenId());
    }

    private User saveUser() {
        var user = User.builder()
                .email("user@test.com")
                .fullName("Test User")
                .password("password")
                .build();
        return userRepository.save(user);
    }

}
