package org.example.library.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.auth.dto.UserRegisterRequest;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.exception.BadRequestException;
import org.example.library.exception.NotFoundException;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.dto.UpdateProfileRequest;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;


    @Test
    void shouldRegisterUser() {
        var request = UserRegisterRequest.builder()
                .email("newuser@example.com")
                .fullName("New User")
                .password("password123")
                .build();

        var response = userService.register(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getFullName()).isEqualTo("New User");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        var savedUser = userRepository.findById(response.getId())
                .orElseThrow(() -> new AssertionError("User not found after registration"));
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void shouldThrowBadRequestWhenRegisteringWithExistingEmail() {
        var existingUser = User.builder()
                .email("existing@example.com")
                .fullName("Existing User")
                .password("pass")
                .role(Role.USER)
                .build();
        userRepository.save(existingUser);
        em.flush();

        var request = UserRegisterRequest.builder()
                .email("existing@example.com")
                .fullName("New User")
                .password("password123")
                .build();

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.auth.email_already_registered");
    }

    @Test
    void shouldUpdateProfile() {
        var user = User.builder()
                .email("user@example.com")
                .fullName("Old Name")
                .password("pass")
                .role(Role.USER)
                .build();
        userRepository.save(user);
        em.flush();
        em.clear();
        var request = UpdateProfileRequest.builder()
                .email("user@example.com")
                .fullName("New Name")
                .build();

        var response = userService.updateProfile(user.getId(), request);

        assertThat(response.userResponse().getFullName()).isEqualTo("New Name");
        assertThat(response.tokenResponse()).isNull();
        var updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AssertionError("User not found after profile update"));
        assertThat(updatedUser.getFullName()).isEqualTo("New Name");
    }

    @Test
    void shouldUpdateProfileAndIssueNewTokensWhenEmailChanges() {
        var user = User.builder()
                .email("old@example.com")
                .fullName("User")
                .password("pass")
                .role(Role.USER)
                .build();
        userRepository.save(user);
        em.flush();
        em.clear();
        var request = UpdateProfileRequest.builder()
                .email("new@example.com")
                .fullName("User")
                .build();

        var response = userService.updateProfile(user.getId(), request);

        assertThat(response.userResponse().getEmail()).isEqualTo("new@example.com");
        assertThat(response.tokenResponse()).isNotNull();
        assertThat(response.tokenResponse().accessToken()).isNotBlank();
        var updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new AssertionError("User not found after email update"));
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void shouldThrowNotFoundWhenUpdatingNonExistentUser() {
        var request = UpdateProfileRequest.builder()
                .email("test@example.com")
                .fullName("Test")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(-1, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.user.not_found");
    }

    @Test
    void shouldThrowBadRequestWhenUpdatingToExistingEmail() {
        var user1 = User.builder()
                .email("user1@example.com")
                .fullName("User One")
                .password("pass")
                .role(Role.USER)
                .build();
        var user2 = User.builder()
                .email("user2@example.com")
                .fullName("User Two")
                .password("pass")
                .role(Role.USER)
                .build();
        userRepository.save(user1);
        userRepository.save(user2);
        em.flush();
        em.clear();
        var request = UpdateProfileRequest.builder()
                .email("user2@example.com")
                .fullName("User One Updated")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(user1.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.auth.email_already_registered");
    }

}
