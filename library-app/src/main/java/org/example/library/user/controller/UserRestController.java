package org.example.library.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.library.security.UserPrincipal;
import org.example.library.security.util.CookieUtils;
import org.example.library.user.dto.UpdateProfileRequest;
import org.example.library.user.dto.UserResponse;
import org.example.library.user.mapper.UserMapper;
import org.example.library.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final CookieUtils cookieUtils;


    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return userMapper.toResponse(userPrincipal);
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                      @Valid @RequestBody UpdateProfileRequest request,
                                      HttpServletResponse response) {
        var authResponse = userService.updateProfile(userPrincipal.getId(), request);
        if (authResponse.tokenResponse() != null) {
            cookieUtils.setTokenCookies(response, authResponse.tokenResponse());
        }
        return authResponse.userResponse();
    }

}
