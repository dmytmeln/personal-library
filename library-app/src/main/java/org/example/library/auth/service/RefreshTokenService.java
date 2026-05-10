package org.example.library.auth.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.example.library.auth.dto.TokenResponse;
import org.example.library.security.jwt.JwtService;
import org.example.library.auth.domain.RefreshToken;
import org.example.library.auth.repository.RefreshTokenRepository;
import org.example.library.user.domain.User;
import org.example.library.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Value("${application.security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;


    @Transactional
    public TokenResponse refreshToken(String rawToken, Integer tokenId) {
        var refreshToken = invalidateToken(rawToken, tokenId);
        return generateNewTokens(refreshToken.getUser());
    }

    @Transactional
    public RefreshToken invalidateToken(String rawToken, Integer tokenId) {
        var refreshToken = repository.findById(tokenId)
                .orElseThrow(() -> new BadCredentialsException("Unknown token"));

        if (refreshToken.isExpired() || !isTokenHashValid(rawToken, refreshToken.getRefreshTokenHash())) {
            throw new BadCredentialsException("Invalid token");
        }

        var email = jwtService.extractEmail(rawToken);
        if (!refreshToken.getUser().getEmail().equals(email)) {
            refreshToken.setRevoked(true);
            throw new SecurityException("Identity mismatch. Please log in again.");
        }

        if (refreshToken.isRevoked()) {
            repository.revokeAllByUserId(refreshToken.getUser().getId());
            throw new SecurityException("Breach detected: Refresh token reused.");
        }

        refreshToken.setRevoked(true);
        return refreshToken;
    }

    @Transactional
    public TokenResponse issueTokensOnEmailUpdate(User user) {
        repository.revokeAllByUserId(user.getId());
        return generateNewTokens(user);
    }

    @Transactional
    public TokenResponse generateNewTokens(User user) {
        var accessToken = jwtService.generateAccessToken(userMapper.toPrincipal(user));
        var rawRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        var refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        var refreshTokenHash = DigestUtils.sha256Hex(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
        refreshToken.setRefreshTokenHash(refreshTokenHash);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshToken.setRevoked(false);
        repository.save(refreshToken);

        return new TokenResponse(accessToken, rawRefreshToken, refreshToken.getId());
    }

    private boolean isTokenHashValid(String rawToken, String storedHash) {
        var rawTokenHash = DigestUtils.sha256Hex(rawToken.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(
                rawTokenHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }

}
