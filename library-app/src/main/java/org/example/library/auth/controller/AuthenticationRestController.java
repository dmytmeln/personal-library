package org.example.library.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.auth.dto.AuthenticationRequest;
import org.example.library.auth.dto.AuthenticationResponse;
import org.example.library.auth.dto.TokenResponse;
import org.example.library.auth.dto.UserRegisterRequest;
import org.example.library.auth.service.AuthService;
import org.example.library.security.jwt.RefreshTokenService;
import org.example.library.user.dto.UserResponse;
import org.example.library.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationRestController {

    private final AuthService service;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid UserRegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/authenticate")
    public AuthenticationResponse authenticate(@RequestBody AuthenticationRequest request) {
        return service.authenticate(request);
    }

    @PostMapping("/refresh")
    public void refreshToken() {
        var tokenResponse = refreshTokenService.refreshToken(null, null);

    }

}
