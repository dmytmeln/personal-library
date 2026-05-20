package org.example.library.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.library.auth.dto.AuthenticationResponse;
import org.example.library.auth.dto.TokenResponse;
import org.example.library.auth.dto.UserRegisterRequest;
import org.example.library.common.exception.BadRequestException;
import org.example.library.common.exception.NotFoundException;
import org.example.library.auth.service.RefreshTokenService;
import org.example.library.user.dto.UpdateProfileRequest;
import org.example.library.user.dto.UserResponse;
import org.example.library.user.mapper.UserMapper;
import org.example.library.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;


    public UserResponse register(UserRegisterRequest request) {
        if (repository.existsByEmail(request.getEmail()))
            throw new BadRequestException("error.auth.email_already_registered");

        var user = mapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var savedUser = repository.save(user);
        log.info("[REGISTER_SUCCESS] User: {}", savedUser.getEmail());

        return mapper.toResponse(savedUser);
    }

    @Transactional
    public AuthenticationResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        var user = repository.findById(userId)
                .orElseThrow(() -> new NotFoundException("error.user.not_found"));

        var oldEmail = user.getEmail();
        if (!oldEmail.equalsIgnoreCase(request.getEmail()) && repository.existsByEmail(request.getEmail()))
            throw new BadRequestException("error.auth.email_already_registered");

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());

        TokenResponse tokenResponse = null;
        if (!oldEmail.equalsIgnoreCase(user.getEmail())) {
            tokenResponse = refreshTokenService.issueTokensOnEmailUpdate(user);
        }

        log.info("[PROFILE_UPDATE_SUCCESS] User ID: {}", userId);
        return new AuthenticationResponse(tokenResponse, mapper.toResponse(user));
    }

}
