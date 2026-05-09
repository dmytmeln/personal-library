package org.example.library.security;

import org.example.library.user.domain.Role;

public interface UserPrincipal {

    Integer getId();

    String getEmail();

    String getFullName();

    Role getRole();

}
