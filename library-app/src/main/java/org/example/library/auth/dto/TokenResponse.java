package org.example.library.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, Integer refreshTokenId) {
}
