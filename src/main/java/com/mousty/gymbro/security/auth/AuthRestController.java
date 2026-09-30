package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.LoginDTO;
import com.mousty.gymbro.dto.user.ResetPasswordDTO;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.request.OTPRequest;
import com.mousty.gymbro.request.RefreshTokenRequest;
import com.mousty.gymbro.response.LoginResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.response.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.mousty.gymbro.security.CurrentUsername;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthRestController {

    private final AuthService authService;
    private final UserService userService;

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

    // JwtFilter already validated the bearer token and populated the security context.
    @GetMapping("/is-authenticated")
    public Boolean isAuthenticated(@CurrentUsername String username) {
        return username != null;
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
    public UserDTO getProfile(@CurrentUsername String username) {
        return userService.getUserWithImageUrl(username);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.logout(request);
    }
}