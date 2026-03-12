package org.example.library.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.auth.dto.AuthenticationRequest;
import org.example.library.auth.dto.AuthenticationResponse;
import org.example.library.security.UserDetailsImpl;
import org.example.library.security.jwt.RefreshTokenService;
import org.example.library.user.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;


    public AuthenticationResponse authenticate(AuthenticationRequest authRequest) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                ));
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        var tokenResponse = refreshTokenService.generateNewTokens(userDetails.user());
        log.info("[LOGIN_SUCCESS] User: {}", authRequest.getEmail());
        return new AuthenticationResponse(tokenResponse, userMapper.toResponse(userDetails));
    }

}
