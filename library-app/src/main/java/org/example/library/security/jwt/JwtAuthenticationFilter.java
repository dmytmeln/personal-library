package org.example.library.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${application.security.jwt.cookie-name}")
    private String cookieName;

    private final AuthenticationManager authenticationManager;


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var jwtToken = getJwtToken(request);
        
        if (jwtToken.isPresent()) {
            log.debug("[JWT] Token extracted from request: {}", request.getRequestURI());
            setSecurityContext(new JwtTokenAuthentication(jwtToken.get()));
        } else {
            log.debug("[JWT] No token found in request: {}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }

    private void setSecurityContext(JwtTokenAuthentication auth) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationManager.authenticate(auth));
        SecurityContextHolder.setContext(context);
    }

    private Optional<String> getJwtToken(HttpServletRequest request) {
        return getJwtTokenFromHeader(request)
                .or(() -> getJwtTokenFromCookies(request));
    }

    private Optional<String> getJwtTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return Optional.empty();

        return Optional.of(authHeader.substring(7));
    }

    private Optional<String> getJwtTokenFromCookies(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null)
            return Optional.empty();

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

}