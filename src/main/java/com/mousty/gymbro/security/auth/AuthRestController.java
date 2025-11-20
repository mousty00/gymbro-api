package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.LoginDTO;
import com.mousty.gymbro.dto.user.ResetPasswordDTO;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.request.OTPRequest;
import com.mousty.gymbro.response.LoginResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.jwt.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthRestController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginDTO request) {
        return authService.login(request);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse signup(@Valid @RequestBody SignupDTO request) {
        return authService.signup(request);
    }

    @PostMapping("/send-reset-otp")
    public void sendResetOtp(@RequestParam String email) {
        authService.sendResetOtp(email);
    }

    @GetMapping("/is-authenticated")
    public Boolean isAuthenticated(@RequestHeader(value = "Authorization") String token) {
        token = token.substring(7);
        return jwtUtil.isValid(token);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/send-otp")
    public MessageResponse sendVerifyOtp(String email) {
        return authService.sendOtp(email);
    }

    @PostMapping("/verify-otp")
    public MessageResponse verifyOtp(
            @Valid @RequestBody OTPRequest otpRequest) {
        return authService.verifyOtp(otpRequest);
    }

    @GetMapping("/profile")
    public UserDTO getProfile(@CurrentSecurityContext(expression = "authentication?.name") String username) {
        return userService.getUserWithImageUrl(username);
    }
}