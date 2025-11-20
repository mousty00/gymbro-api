package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.email.EmailService;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.repository.UserRepository;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.dto.user.LoginDTO;
import com.mousty.gymbro.dto.user.ResetPasswordDTO;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.request.OTPRequest;
import com.mousty.gymbro.response.LoginResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtTokenProvider;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginDTO request) {
        try {
            final Authentication authentication = authenticate(request.getUsername(), request.getPassword());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDTO user = userService.getUserByUsername(request.getUsername());
            final String token = jwtTokenProvider.generateToken(user);

            return LoginResponse.builder()
                            .message("login successful")
                            .token(token)
                            .result(user)
                            .build();

        } catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException("Username not found");
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid password");
        }
    }

    @Transactional
    public MessageResponse signup(SignupDTO request) {
        userService.createUser(request, passwordEncoder);
        emailService.sendWelcomeEmail(request.getEmail(), request.getFirstName());
        sendOtp(request.getUsername());

        return MessageResponse.builder()
                        .message("Signup successful!")
                        .build();
    }

    private Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
                )
        );
    }

    public void sendResetOtp(String email) {
        final User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email %s".formatted(email)));
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 10000000));
        Long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);
        user.setResetOtp(otp);
        user.setResetOtpExpiredAt(expiryTime);
        userRepository.save(user);
        emailService.sendResetOtpEmail(email, user.getFirstName(), otp);
    }

    public MessageResponse resetPassword(ResetPasswordDTO request) {
        final User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email %s".formatted(request.getEmail())));
        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        if (user.getResetOtpExpiredAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("OTP has expired");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiredAt(0L);

        userRepository.save(user);
        return MessageResponse.builder()
                .message("Password reset it successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    public MessageResponse sendOtp(String username) {
        final User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(username)));

        if(user.getIsAccountVerified() != null && user.getIsAccountVerified()) {
            throw new IllegalArgumentException("User is already verified");
        }
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        Long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        user.setVerifyOtp(otp);
        user.setVerifyOtpExpiredAt(expiryTime);
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp);

        return MessageResponse.builder()
                .message("Account Verification Email sent successfully")
                .build();
    }

    public MessageResponse verifyOtp(OTPRequest otpRequest) {
        final User user = userRepository.findUserByUsername(otpRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(otpRequest.username())));
        if(user.getIsAccountVerified() != null && user.getIsAccountVerified()) {
            throw new IllegalArgumentException("User is already verified");
        }
        if(user.getVerifyOtp() == null || !user.getVerifyOtp().equals(otpRequest.otp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        if(user.getVerifyOtpExpiredAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("OTP has expired");
        }
        user.setIsAccountVerified(true);
        user.setVerifyOtp(null);
        user.setVerifyOtpExpiredAt(0L);
        userRepository.save(user);
        return MessageResponse.builder()
                .message("Account Verified successfully")
                .build();
    }

    public UserDTO getProfile(String username) {
        return userService.getUserWithImageUrl(username);
    }

    public void checkAuthorization(UUID userId, String username, String errorMessage){
        final String loggedUsername = userService.getUsernameById(userId);
        if(!loggedUsername.equals(username)){
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}
