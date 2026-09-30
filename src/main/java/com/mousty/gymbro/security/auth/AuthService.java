package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.email.EmailService;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.repository.UserRepository;
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
import com.mousty.gymbro.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCK_DURATION_MS = 15 * 60 * 1000;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final long OTP_LOCK_DURATION_MS = 15 * 60 * 1000;

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtTokenProvider;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.secret}")
    private String otpKey;

    // noRollbackFor: the failed-attempt counter is saved right before these are thrown;
    // rolling it back would make the lockout never trigger.
    @Transactional(noRollbackFor = {BadCredentialsException.class, AuthException.class})
    public LoginResponse login(LoginDTO request) {
        final Optional<User> userOpt = userRepository.findUserByUsername(request.username());

        userOpt.ifPresent(this::checkLoginLock);

        try {
            final Authentication authentication = authenticate(request.username(), request.password());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            final User user = userOpt.orElseThrow(() -> new UsernameNotFoundException("Username not found"));
            user.setFailedLoginAttempts(0);
            user.setLoginLockedUntil(null);
            userRepository.save(user);
            // checked only after the password is correct, so it reveals nothing to someone guessing
            requireVerified(user);

            UserDTO userDTO = userService.getUserByUsername(request.username());
            final String token = jwtTokenProvider.generateToken(userDTO);
            final String refreshToken = refreshTokenService.issue(user);

            return LoginResponse.builder()
                            .message("login successful")
                            .token(token)
                            .refreshToken(refreshToken)
                            .result(userDTO)
                            .build();

        } catch (UsernameNotFoundException | BadCredentialsException e) {
            // Same error for unknown user and wrong password: no username enumeration.
            userOpt.ifPresent(this::registerFailedLoginAttempt);
            log.warn("Failed login for '{}' from {}", request.username(), clientIp());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Transactional
    public MessageResponse signup(SignupDTO request) {
        userService.createUser(request);
        emailService.sendWelcomeEmail(request.email(), request.firstName());
        sendOtp(request.email());

        return MessageResponse.builder()
                        .message("Signup successful!")
                        .build();
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        final User user = refreshTokenService.validateAndRevoke(request.refreshToken());
        requireVerified(user);
        final UserDTO userDTO = userService.getUserByUsername(user.getUsername());
        final String newAccessToken = jwtTokenProvider.generateToken(userDTO);
        final String newRefreshToken = refreshTokenService.issue(user);

        return TokenResponse.builder()
                .message("Token refreshed successfully")
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public MessageResponse logout(RefreshTokenRequest request) {
        refreshTokenService.validateAndRevoke(request.refreshToken());
        return MessageResponse.builder()
                .message("Logged out successfully")
                .timestamp(Instant.now())
                .build();
    }

    private Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
    }

    private void checkLoginLock(User user) {
        if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil() > System.currentTimeMillis()) {
            long retryAfterSeconds = (user.getLoginLockedUntil() - System.currentTimeMillis()) / 1000;
            throw AuthException.accountLocked(retryAfterSeconds);
        }
    }

    private void registerFailedLoginAttempt(User user) {
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLoginLockedUntil(System.currentTimeMillis() + LOGIN_LOCK_DURATION_MS);
        }
        userRepository.save(user);
    }

    private void checkOtpLock(User user) {
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil() > System.currentTimeMillis()) {
            long retryAfterSeconds = (user.getOtpLockedUntil() - System.currentTimeMillis()) / 1000;
            throw AuthException.accountLocked(retryAfterSeconds);
        }
    }

    private void registerFailedOtpAttempt(User user) {
        int attempts = (user.getOtpFailedAttempts() == null ? 0 : user.getOtpFailedAttempts()) + 1;
        user.setOtpFailedAttempts(attempts);
        if (attempts >= MAX_OTP_ATTEMPTS) {
            user.setOtpLockedUntil(System.currentTimeMillis() + OTP_LOCK_DURATION_MS);
            user.setResetOtp(null);
            user.setVerifyOtp(null);
        }
        userRepository.save(user);
        log.warn("Failed OTP attempt {} for '{}' from {}", attempts, user.getUsername(), clientIp());
    }

    private void resetOtpLock(User user) {
        user.setOtpFailedAttempts(0);
        user.setOtpLockedUntil(null);
    }

    public void sendResetOtp(String email) {
        // Silent on unknown email: the response must not reveal which emails have accounts.
        final Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        final User user = userOpt.get();
        String otp = newOtp();
        Long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);
        user.setResetOtp(hashOtp(otp));
        user.setResetOtpExpiredAt(expiryTime);
        userRepository.save(user);
        emailService.sendResetOtpEmail(email, user.getFirstName(), otp);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public MessageResponse resetPassword(ResetPasswordDTO request) {
        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> AuthException.notFound(request.email()));
        checkOtpLock(user);
        if (user.getResetOtpExpiredAt() == null || user.getResetOtpExpiredAt() < System.currentTimeMillis()) {
            throw AuthException.expiredOtp();
        }
        if (!otpMatches(user.getResetOtp(), request.otp())) {
            registerFailedOtpAttempt(user);
            throw AuthException.invalidOtp();
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiredAt(0L);
        resetOtpLock(user);

        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user);
        return MessageResponse.builder()
                .message("Password reset successfully!")
                .timestamp(Instant.now())
                .build();
    }

    @Transactional
    /** (Re)sends the verification code. Same response for unknown and already-verified emails: no enumeration. */
    public MessageResponse sendOtp(String email) {
        final MessageResponse response = MessageResponse.builder()
                .message("If the account exists and is not verified, a verification email has been sent")
                .build();
        final Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || Boolean.TRUE.equals(userOpt.get().getIsAccountVerified())) {
            return response;
        }
        final User user = userOpt.get();
        String otp = newOtp();
        Long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        user.setVerifyOtp(hashOtp(otp));
        user.setVerifyOtpExpiredAt(expiryTime);
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getFirstName(), otp);
        return response;
    }

    private static void requireVerified(User user) {
        if (!Boolean.TRUE.equals(user.getIsAccountVerified())) {
            throw AuthException.emailNotVerified();
        }
    }

    @Transactional(noRollbackFor = AuthException.class)
    public MessageResponse verifyOtp(OTPRequest otpRequest) {
        final User user = userRepository.findUserByUsername(otpRequest.username())
                .orElseThrow(() -> new UsernameNotFoundException("User %s not found".formatted(otpRequest.username())));
        if (user.getIsAccountVerified() != null && user.getIsAccountVerified()) {
            throw AuthException.alreadyVerified();
        }
        checkOtpLock(user);
        if (user.getVerifyOtpExpiredAt() == null || user.getVerifyOtpExpiredAt() < System.currentTimeMillis()) {
            throw AuthException.expiredOtp();
        }
        if (!otpMatches(user.getVerifyOtp(), otpRequest.otp())) {
            registerFailedOtpAttempt(user);
            throw AuthException.invalidOtp();
        }
        user.setIsAccountVerified(true);
        user.setVerifyOtp(null);
        user.setVerifyOtpExpiredAt(0L);
        resetOtpLock(user);
        userRepository.save(user);
        return MessageResponse.builder()
                .message("Account verified successfully")
                .build();
    }

    public UserDTO getProfile(String username) {
        return userService.getUserWithImageUrl(username);
    }

    public void checkAuthorization(UUID resourceOwnerId, String currentUsername, String errorMessage) {
        final String ownerUsername = userService.getUsernameById(resourceOwnerId);
        if (currentUsername == null || !ownerUsername.equals(currentUsername)) {
            throw AuthException.forbidden(errorMessage);
        }
    }

    /** Ownership check against the stored owner of an already-loaded entity. */
    public void checkAuthorization(User owner, String currentUsername, String errorMessage) {
        if (owner == null || currentUsername == null || !owner.getUsername().equals(currentUsername)) {
            throw AuthException.forbidden(errorMessage);
        }
    }

    /** Username from the verified bearer token; null when the caller is anonymous. */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private static String newOtp() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }

    // OTPs are stored as HMAC-SHA256 keyed with the server secret: a plain hash of a 6-digit
    // code would be reversible offline in about a million tries if the DB leaked.
    private String hashOtp(String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(otpKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private boolean otpMatches(String storedHash, String given) {
        return storedHash != null && given != null && MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8), hashOtp(given).getBytes(StandardCharsets.UTF_8));
    }

    // Resolved by Tomcat's RemoteIpValve (server.forward-headers-strategy=native), not raw X-Forwarded-For.
    private static String clientIp() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest().getRemoteAddr()
                : "unknown";
    }
}
