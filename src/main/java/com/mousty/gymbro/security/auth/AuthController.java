package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.dto.user.LoginDTO;
import com.mousty.gymbro.dto.user.ResetPasswordDTO;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.request.OTPRequest;
import com.mousty.gymbro.request.RefreshTokenRequest;
import com.mousty.gymbro.response.LoginResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.response.TokenResponse;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.InputArgument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@DgsComponent
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @DgsMutation
    public LoginResponse login(@Valid @InputArgument LoginDTO credentials) {
        return service.login(credentials);
    }

    @DgsMutation
    public MessageResponse signup(@Valid @InputArgument SignupDTO credentials) {
        return service.signup(credentials);
    }

    @DgsMutation
    public MessageResponse sendResetOtp(@InputArgument String email) {
        service.sendResetOtp(email);
        return MessageResponse.builder()
                .message("OTP sent successfully!")
                .build();
    }

    @DgsMutation
    public MessageResponse resetPassword(@Valid @InputArgument ResetPasswordDTO request) {
        return service.resetPassword(request);
    }

    @DgsMutation
    public MessageResponse sendVerifyOtp(@InputArgument String email) {
        return service.sendOtp(email);
    }

    @DgsMutation
    public MessageResponse verifyOtp(@Valid @InputArgument OTPRequest request) {
        return service.verifyOtp(request);
    }

    @DgsMutation
    public TokenResponse refreshToken(@Valid @InputArgument RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @DgsMutation
    public MessageResponse logout(@Valid @InputArgument RefreshTokenRequest request) {
        return service.logout(request);
    }

}
