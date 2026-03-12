package org.example.library.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements AuthenticationProvider {

    private final JwtService jwtService;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        var jwtAuth = (JwtTokenAuthentication) authentication;
        var token = jwtAuth.getToken();
        jwtAuth.clearCredentials();
        if (!jwtService.isTokenValid(token))
            throw new BadCredentialsException("error.auth.invalid_jwt_token");

        var userPrincipal = jwtService.extractUserPrincipal(token);
        return JwtTokenAuthentication.authenticated(userPrincipal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtTokenAuthentication.class.isAssignableFrom(authentication);
    }

}