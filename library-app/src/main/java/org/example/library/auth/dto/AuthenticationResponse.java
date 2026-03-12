package org.example.library.auth.dto;

import org.example.library.user.dto.UserResponse;

public record AuthenticationResponse(TokenResponse tokenResponse, UserResponse userResponse) {
}
