package org.example.library.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.library.user.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
@Builder
@Getter
public class JwtUserPrincipal implements UserPrincipal, UserDetails {

    private final Integer id;
    private final String email;
    private final String fullName;
    private final Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

}
