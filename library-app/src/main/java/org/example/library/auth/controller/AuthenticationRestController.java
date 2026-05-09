package org.example.library.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.auth.dto.AuthenticationRequest;
import org.example.library.auth.dto.UserRegisterRequest;
import org.example.library.auth.service.AuthService;
import org.example.library.security.jwt.RefreshTokenService;
import org.example.library.security.util.CookieUtils;
import org.example.library.user.dto.UserResponse;
import org.example.library.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationRestController {

    private final AuthService service;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtils cookieUtils;


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody @Valid UserRegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/authenticate")
    public UserResponse authenticate(@RequestBody AuthenticationRequest request, HttpServletResponse response) {
        var authenticationResponse = service.authenticate(request);
        cookieUtils.setTokenCookies(response, authenticationResponse.tokenResponse());
        return authenticationResponse.userResponse();
    }

    @PostMapping("/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        var rawToken = getRawToken(request);
        var tokenId = getTokenId(request);
        var tokenResponse = refreshTokenService.refreshToken(rawToken, tokenId);
        cookieUtils.setTokenCookies(response, tokenResponse);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var rawToken = getRawToken(request);
        var tokenId = getTokenId(request);
        refreshTokenService.invalidateToken(rawToken, tokenId);
        cookieUtils.clearTokenCookies(response);
    }

    private Integer getTokenId(HttpServletRequest request) {
        return cookieUtils.getCookieValue(request, cookieUtils.getRefreshTokenIdCookieName())
                .map(Integer::parseInt)
                .orElseThrow(() -> new BadCredentialsException("Refresh token ID missing"));
    }

    private String getRawToken(HttpServletRequest request) {
        return cookieUtils.getCookieValue(request, cookieUtils.getRefreshTokenCookieName())
                .orElseThrow(() -> new BadCredentialsException("Refresh token missing"));
    }

}
