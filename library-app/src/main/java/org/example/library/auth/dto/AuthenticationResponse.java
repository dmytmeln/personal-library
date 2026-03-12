package org.example.library.auth.dto;

import org.example.library.user.dto.UserResponse;

public record AuthenticationResponse(String accessToken, String refreshToken, UserResponse userResponse) {
}
