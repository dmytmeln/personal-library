package org.example.library.security.jwt;

import lombok.RequiredArgsConstructor;
import org.example.library.auth.dto.TokenResponse;
import org.example.library.user.domain.User;
import org.example.library.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
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

        var preHashed = DigestUtils.md5DigestAsHex(rawToken.getBytes(StandardCharsets.UTF_8));
        if (refreshToken.isExpired() || !passwordEncoder.matches(preHashed, refreshToken.getRefreshTokenHash())) {
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
        var preHashed = DigestUtils.md5DigestAsHex(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
        refreshToken.setRefreshTokenHash(passwordEncoder.encode(preHashed));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshToken.setRevoked(false);
        repository.save(refreshToken);

        return new TokenResponse(accessToken, rawRefreshToken, refreshToken.getId());
    }

}
