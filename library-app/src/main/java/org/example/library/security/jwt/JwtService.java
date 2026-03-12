package org.example.library.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.library.security.JwtUserPrincipal;
import org.example.library.security.UserPrincipal;
import org.example.library.user.domain.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;


    public String extractEmail(String refreshToken) {
        var claims = extractAllClaims(refreshToken);
        return claims.getSubject();
    }

    public UserPrincipal extractUserPrincipal(String accessToken) {
        var claims = extractAllClaims(accessToken);
        return JwtUserPrincipal.builder()
                .id(claims.get("id", Integer.class))
                .email(claims.getSubject())
                .fullName(claims.get("fullName", String.class))
                .role(Role.valueOf(claims.get("role", String.class)))
                .build();
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        Map<String, Object> extraClaims = Map.of(
                "id", userPrincipal.getId(),
                "role", userPrincipal.getRole().name(),
                "fullName", userPrincipal.getFullName()
        );
        return generateToken(extraClaims, userPrincipal.getEmail(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(String email) {
        return generateToken(Collections.emptyMap(), email, refreshTokenExpirationMs);
    }

    private String generateToken(Map<String, ?> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            log.error("Invalid JWT accessToken: {}", e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("JWT accessToken is null or empty: {}", e.getMessage());
        }

        return false;
    }

    private void parseToken(String token) {
        Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parse(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}