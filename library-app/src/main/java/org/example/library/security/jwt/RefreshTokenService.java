package org.example.library.security.jwt;

import lombok.RequiredArgsConstructor;
import org.example.library.auth.dto.TokenResponse;
import org.example.library.user.domain.User;
import org.example.library.user.mapper.UserMapper;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;


    @Transactional
    public TokenResponse refreshToken(String rawToken, Integer tokenId) {
        var refreshToken = repository.findById(tokenId)
                .orElseThrow(() -> new BadCredentialsException("Unknown token"));

        // todo localization
        if (refreshToken.isExpired() || !passwordEncoder.matches(rawToken, refreshToken.getRefreshTokenHash())) {
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
        return generateNewTokens(refreshToken.getUser());
    }

    @Transactional
    public TokenResponse issueTokensOnEmailUpdate(User user) {
        repository.revokeAllByUserId(user.getId());
        return generateNewTokens(user);
    }

    private TokenResponse generateNewTokens(User user) {
        var accessToken = jwtService.generateAccessToken(userMapper.toPrincipal(user));
        var refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new TokenResponse(accessToken, refreshToken);
    }

}
