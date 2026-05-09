package org.example.library.security.jwt;

import lombok.Getter;
import org.example.library.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JwtTokenAuthentication implements Authentication {

    @Getter
    private String token;
    private UserPrincipal userPrincipal;
    private boolean authenticated;

    private JwtTokenAuthentication(String token) {
        this.token = token;
    }

    private JwtTokenAuthentication(UserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
        this.authenticated = true;
    }

    public static JwtTokenAuthentication authenticated(UserPrincipal userPrincipal) {
        return new JwtTokenAuthentication(userPrincipal);
    }

    public static JwtTokenAuthentication unauthenticated(String token) {
        return new JwtTokenAuthentication(token);
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Optional.ofNullable(userPrincipal)
                .map(UserPrincipal::getRole)
                .map(role -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())))
                .orElseGet(List::of);
    }

    @Override
    public String getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public UserPrincipal getPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Not supported, use constructor");
        }
    }

    @Override
    public String getName() {
        return Optional.ofNullable(userPrincipal)
                .map(UserPrincipal::getEmail)
                .orElse(null);
    }

    public void clearCredentials() {
        this.token = null;
    }

}