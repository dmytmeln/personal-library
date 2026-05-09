package org.example.library.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.example.library.auth.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Getter
@Component
public class CookieUtils {

    @Value("${application.security.jwt.access-cookie-name}")
    private String accessTokenCookieName;

    @Value("${application.security.jwt.refresh-cookie-name}")
    private String refreshTokenCookieName;

    @Value("${application.security.jwt.refresh-id-cookie-name}")
    private String refreshTokenIdCookieName;

    @Value("${application.security.jwt.access-expiration}")
    private long accessTokenExpirationMs;

    @Value("${application.security.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;


    public void setTokenCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        setCookie(response, accessTokenCookieName, tokenResponse.accessToken(), accessTokenExpirationMs);
        setCookie(response, refreshTokenCookieName, tokenResponse.refreshToken(), refreshTokenExpirationMs);
        setCookie(response, refreshTokenIdCookieName, String.valueOf(tokenResponse.refreshTokenId()), refreshTokenExpirationMs);
    }

    public void clearTokenCookies(HttpServletResponse response) {
        deleteCookie(response, accessTokenCookieName);
        deleteCookie(response, refreshTokenCookieName);
        deleteCookie(response, refreshTokenIdCookieName);
    }

    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeMs) {
        var cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeMs / 1000)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        var cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

}
