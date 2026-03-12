package org.example.library.user.dto;

import lombok.Builder;

@Builder
public record UserUpdateResponse(UserResponse user, String accessToken, String refreshToken) {
}
