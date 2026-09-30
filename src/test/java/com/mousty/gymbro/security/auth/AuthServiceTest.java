package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.dto.user.LoginDTO;
import com.mousty.gymbro.dto.user.ResetPasswordDTO;
import com.mousty.gymbro.dto.user.SignupDTO;
import com.mousty.gymbro.dto.user.UserDTO;
import com.mousty.gymbro.email.EmailService;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.exception.UserException;
import com.mousty.gymbro.repository.UserRepository;
import com.mousty.gymbro.request.OTPRequest;
import com.mousty.gymbro.request.RefreshTokenRequest;
import com.mousty.gymbro.response.LoginResponse;
import com.mousty.gymbro.response.MessageResponse;
import com.mousty.gymbro.response.TokenResponse;
import com.mousty.gymbro.security.jwt.JwtUtil;
import com.mousty.gymbro.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String USERNAME = "mousty";
    private static final String EMAIL = "mousty@example.com";
    private static final long FIFTEEN_MIN_MS = 15 * 60 * 1000L;

    @Mock private UserService userService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpKey", "test-secret-key-for-hmac-otp-hashing");
        user = User.builder()
                .id(UUID.randomUUID())
                .username(USERNAME)
                .email(EMAIL)
                .firstName("Mousty")
                .password("encoded-old")
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static UserDTO dto() {
        return UserDTO.builder().username(USERNAME).email(EMAIL).build();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** Runs sendResetOtp and returns the plain OTP that was emailed. */
    private String issueResetOtp() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        authService.sendResetOtp(EMAIL);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendResetOtpEmail(eq(EMAIL), eq("Mousty"), otp.capture());
        return otp.getValue();
    }

    /** Runs sendOtp and returns the plain OTP that was emailed. */
    private String issueVerifyOtp() {
        when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
        authService.sendOtp(USERNAME);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(EMAIL), eq("Mousty"), otp.capture());
        return otp.getValue();
    }

    @Nested
    @DisplayName("login")
    class Login {

        private final LoginDTO request = new LoginDTO(USERNAME, "secret");

        @Test
        @DisplayName("success resets the lockout counters and returns access + refresh tokens")
        void success() {
            user.setFailedLoginAttempts(3);
            user.setLoginLockedUntil(now() - 1000);
            Authentication auth = new UsernamePasswordAuthenticationToken(USERNAME, null, List.of());
            UserDTO userDTO = dto();
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userService.getUserByUsername(USERNAME)).thenReturn(userDTO);
            when(jwtUtil.generateToken(userDTO)).thenReturn("jwt");
            when(refreshTokenService.issue(user)).thenReturn("refresh");

            LoginResponse response = authService.login(request);

            assertThat(response.token()).isEqualTo("jwt");
            assertThat(response.refreshToken()).isEqualTo("refresh");
            assertThat(response.result()).isSameAs(userDTO);
            assertThat(user.getFailedLoginAttempts()).isZero();
            assertThat(user.getLoginLockedUntil()).isNull();
            verify(userRepository).save(user);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
        }

        @Test
        @DisplayName("unknown user throws the generic BadCredentialsException")
        void unknownUser() {
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());
            when(authenticationManager.authenticate(any())).thenThrow(new UsernameNotFoundException("nope"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");
            verify(userRepository, never()).save(any());
            verifyNoInteractions(refreshTokenService, jwtUtil);
        }

        @Test
        @DisplayName("wrong password throws the same generic message and increments the counter")
        void wrongPassword() {
            user.setFailedLoginAttempts(1);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid username or password");
            assertThat(user.getFailedLoginAttempts()).isEqualTo(2);
            assertThat(user.getLoginLockedUntil()).isNull();
            verify(userRepository).save(user);
            verifyNoInteractions(refreshTokenService, jwtUtil);
        }

        @Test
        @DisplayName("null counter is treated as zero on failure")
        void nullCounter() {
            user.setFailedLoginAttempts(null);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

            assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
            assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("5th failure locks the account for ~15 minutes")
        void fifthFailureLocks() {
            user.setFailedLoginAttempts(4);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

            assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
            assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
            assertThat(user.getLoginLockedUntil()).isCloseTo(now() + FIFTEEN_MIN_MS, within(5000L));
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("locked account throws 429 before authenticating")
        void lockedAccount() {
            user.setLoginLockedUntil(now() + 60_000);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            verifyNoInteractions(authenticationManager);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("expired lock no longer blocks login")
        void expiredLock() {
            user.setLoginLockedUntil(now() - 1000);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

            assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
            verify(authenticationManager).authenticate(any());
        }
    }

    @Nested
    @DisplayName("@Transactional(noRollbackFor) keeps failed-attempt counters")
    class NoRollback {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"login", "resetPassword", "verifyOtp"})
        @DisplayName("counter-saving methods do not roll back on AuthException")
        void noRollbackForAuthException(String methodName) throws Exception {
            Transactional tx = findMethod(methodName).getAnnotation(Transactional.class);
            assertThat(tx).isNotNull();
            assertThat(tx.noRollbackFor()).contains(AuthException.class);
        }

        @Test
        @DisplayName("login does not roll back on BadCredentialsException")
        void loginNoRollbackForBadCredentials() throws Exception {
            Transactional tx = findMethod("login").getAnnotation(Transactional.class);
            assertThat(tx.noRollbackFor()).contains(BadCredentialsException.class);
        }

        private java.lang.reflect.Method findMethod(String name) {
            for (var m : AuthService.class.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            throw new AssertionError("no method " + name);
        }
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("creates the user, sends welcome email and verification OTP")
        void success() {
            SignupDTO request = SignupDTO.builder()
                    .username(USERNAME).email(EMAIL).password("password1").firstName("Mousty")
                    .birthDate(LocalDate.of(1995, 1, 1)).build();
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            MessageResponse response = authService.signup(request);

            assertThat(response.message()).isEqualTo("Signup successful!");
            verify(userService).createUser(request);
            verify(emailService).sendWelcomeEmail(EMAIL, "Mousty");
            verify(emailService).sendOtpEmail(eq(EMAIL), eq("Mousty"), anyString());
        }

        @Test
        @DisplayName("duplicate user: nothing is emailed")
        void duplicate() {
            SignupDTO request = SignupDTO.builder().username(USERNAME).email(EMAIL).build();
            when(userService.createUser(request)).thenThrow(UserException.alreadyExists("Username", USERNAME));

            assertThatThrownBy(() -> authService.signup(request)).isInstanceOf(UserException.class);
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("revokes the old refresh token then issues a new pair")
        void success() {
            UserDTO userDTO = dto();
            when(refreshTokenService.validateAndRevoke("old")).thenReturn(user);
            when(userService.getUserByUsername(USERNAME)).thenReturn(userDTO);
            when(jwtUtil.generateToken(userDTO)).thenReturn("jwt");
            when(refreshTokenService.issue(user)).thenReturn("new");

            TokenResponse response = authService.refresh(new RefreshTokenRequest("old"));

            assertThat(response.token()).isEqualTo("jwt");
            assertThat(response.refreshToken()).isEqualTo("new");
            var inOrder = org.mockito.Mockito.inOrder(refreshTokenService);
            inOrder.verify(refreshTokenService).validateAndRevoke("old");
            inOrder.verify(refreshTokenService).issue(user);
        }

        @Test
        @DisplayName("invalid refresh token: no new tokens are issued")
        void invalid() {
            when(refreshTokenService.validateAndRevoke("bad")).thenThrow(AuthException.invalidRefreshToken());

            assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("bad")))
                    .isInstanceOf(AuthException.class);
            verify(refreshTokenService, never()).issue(any());
            verifyNoInteractions(jwtUtil);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("revokes the refresh token")
        void success() {
            MessageResponse response = authService.logout(new RefreshTokenRequest("tok"));

            assertThat(response.message()).isEqualTo("Logged out successfully");
            verify(refreshTokenService).validateAndRevoke("tok");
        }

        @Test
        @DisplayName("invalid token propagates AuthException")
        void invalid() {
            when(refreshTokenService.validateAndRevoke("bad")).thenThrow(AuthException.invalidRefreshToken());

            assertThatThrownBy(() -> authService.logout(new RefreshTokenRequest("bad")))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("sendResetOtp")
    class SendResetOtp {

        @Test
        @DisplayName("unknown email returns silently without sending anything")
        void unknownEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatCode(() -> authService.sendResetOtp(EMAIL)).doesNotThrowAnyException();
            verifyNoInteractions(emailService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("stores a hash of the emailed OTP, never the OTP itself")
        void storesHash() {
            String otp = issueResetOtp();

            assertThat(otp).matches("\\d{6}");
            assertThat(user.getResetOtp()).isNotNull().isNotEqualTo(otp).doesNotMatch("\\d{6}");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("OTP expires in about 15 minutes")
        void expiry() {
            issueResetOtp();

            assertThat(user.getResetOtpExpiredAt()).isCloseTo(now() + FIFTEEN_MIN_MS, within(5000L));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("correct OTP resets password, clears OTP and lock, revokes refresh tokens")
        void success() {
            String otp = issueResetOtp();
            user.setOtpFailedAttempts(2);
            when(passwordEncoder.encode("newPassword1")).thenReturn("encoded-new");

            MessageResponse response = authService.resetPassword(new ResetPasswordDTO(EMAIL, otp, "newPassword1"));

            assertThat(response.message()).isEqualTo("Password reset successfully!");
            assertThat(user.getPassword()).isEqualTo("encoded-new");
            assertThat(user.getResetOtp()).isNull();
            assertThat(user.getResetOtpExpiredAt()).isZero();
            assertThat(user.getOtpFailedAttempts()).isZero();
            assertThat(user.getOtpLockedUntil()).isNull();
            verify(refreshTokenService).revokeAllForUser(user);
        }

        @Test
        @DisplayName("unknown email throws AuthException 404")
        void unknownEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, "123456", "x")))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("wrong OTP increments the failed counter and changes nothing else")
        void wrongOtp() {
            String otp = issueResetOtp();
            String wrong = otp.equals("000000") ? "111111" : "000000";

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, wrong, "newPassword1")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid OTP");
            assertThat(user.getOtpFailedAttempts()).isEqualTo(1);
            assertThat(user.getOtpLockedUntil()).isNull();
            assertThat(user.getPassword()).isEqualTo("encoded-old");
            verifyNoInteractions(passwordEncoder, refreshTokenService);
        }

        @Test
        @DisplayName("5th wrong OTP locks and clears both stored OTPs")
        void fifthWrongOtpLocks() {
            user.setResetOtp("some-hash");
            user.setVerifyOtp("other-hash");
            user.setResetOtpExpiredAt(now() + 60_000);
            user.setOtpFailedAttempts(4);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, "123456", "x")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid OTP");
            assertThat(user.getOtpFailedAttempts()).isEqualTo(5);
            assertThat(user.getOtpLockedUntil()).isCloseTo(now() + FIFTEEN_MIN_MS, within(5000L));
            assertThat(user.getResetOtp()).isNull();
            assertThat(user.getVerifyOtp()).isNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("locked account throws 429 even with the right OTP")
        void locked() {
            String otp = issueResetOtp();
            user.setOtpLockedUntil(now() + 60_000);

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, otp, "newPassword1")))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            verifyNoInteractions(passwordEncoder, refreshTokenService);
        }

        @Test
        @DisplayName("expired OTP is rejected before comparing: a wrong guess does not count")
        void expiredCheckedFirst() {
            user.setResetOtp("some-hash");
            user.setResetOtpExpiredAt(now() - 1000);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, "123456", "x")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("OTP has expired");
            assertThat(user.getOtpFailedAttempts()).isZero();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("no expiry stored counts as expired")
        void nullExpiry() {
            user.setResetOtp("some-hash");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, "123456", "x")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("OTP has expired");
        }

        @Test
        @DisplayName("null stored OTP never matches")
        void nullStoredOtp() {
            user.setResetOtp(null);
            user.setResetOtpExpiredAt(now() + 60_000);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, "123456", "x")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid OTP");
            assertThat(user.getOtpFailedAttempts()).isEqualTo(1);
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("null given OTP never matches")
        void nullGivenOtp() {
            issueResetOtp();

            assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordDTO(EMAIL, null, "x")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid OTP");
        }
    }

    @Nested
    @DisplayName("sendOtp")
    class SendOtp {

        @Test
        @DisplayName("stores a hash of the emailed OTP with a ~24h expiry")
        void storesHash() {
            String otp = issueVerifyOtp();

            assertThat(otp).matches("\\d{6}");
            assertThat(user.getVerifyOtp()).isNotNull().isNotEqualTo(otp).doesNotMatch("\\d{6}");
            assertThat(user.getVerifyOtpExpiredAt()).isCloseTo(now() + 24 * 60 * 60 * 1000L, within(5000L));
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("already verified account throws alreadyVerified and sends nothing")
        void alreadyVerified() {
            user.setIsAccountVerified(true);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.sendOtp(USERNAME))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("User is already verified");
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("unknown user throws UsernameNotFoundException")
        void unknownUser() {
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.sendOtp(USERNAME))
                    .isInstanceOf(UsernameNotFoundException.class);
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {

        @Test
        @DisplayName("correct OTP verifies the account and clears OTP and lock")
        void success() {
            String otp = issueVerifyOtp();
            user.setOtpFailedAttempts(3);

            MessageResponse response = authService.verifyOtp(new OTPRequest(USERNAME, otp));

            assertThat(response.message()).isEqualTo("Account verified successfully");
            assertThat(user.getIsAccountVerified()).isTrue();
            assertThat(user.getVerifyOtp()).isNull();
            assertThat(user.getVerifyOtpExpiredAt()).isZero();
            assertThat(user.getOtpFailedAttempts()).isZero();
            assertThat(user.getOtpLockedUntil()).isNull();
        }

        @Test
        @DisplayName("unknown user throws UsernameNotFoundException")
        void unknownUser() {
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, "123456")))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("already verified account throws alreadyVerified")
        void alreadyVerified() {
            user.setIsAccountVerified(true);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, "123456")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("User is already verified");
        }

        @Test
        @DisplayName("wrong OTP increments the failed counter")
        void wrongOtp() {
            String otp = issueVerifyOtp();
            String wrong = otp.equals("000000") ? "111111" : "000000";

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, wrong)))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid OTP");
            assertThat(user.getOtpFailedAttempts()).isEqualTo(1);
            assertThat(user.getIsAccountVerified()).isFalse();
        }

        @Test
        @DisplayName("5th wrong OTP locks and clears both stored OTPs")
        void fifthWrongOtpLocks() {
            user.setVerifyOtp("some-hash");
            user.setResetOtp("other-hash");
            user.setVerifyOtpExpiredAt(now() + 60_000);
            user.setOtpFailedAttempts(4);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, "123456")))
                    .isInstanceOf(AuthException.class);
            assertThat(user.getOtpFailedAttempts()).isEqualTo(5);
            assertThat(user.getOtpLockedUntil()).isCloseTo(now() + FIFTEEN_MIN_MS, within(5000L));
            assertThat(user.getVerifyOtp()).isNull();
            assertThat(user.getResetOtp()).isNull();
        }

        @Test
        @DisplayName("locked account throws 429 even with the right OTP")
        void locked() {
            String otp = issueVerifyOtp();
            user.setOtpLockedUntil(now() + 60_000);

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, otp)))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(user.getIsAccountVerified()).isFalse();
        }

        @Test
        @DisplayName("expired OTP is rejected before comparing: a wrong guess does not count")
        void expiredCheckedFirst() {
            user.setVerifyOtp("some-hash");
            user.setVerifyOtpExpiredAt(now() - 1000);
            when(userRepository.findUserByUsername(USERNAME)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyOtp(new OTPRequest(USERNAME, "123456")))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("OTP has expired");
            assertThat(user.getOtpFailedAttempts()).isZero();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("delegates to userService.getUserWithImageUrl")
        void delegates() {
            UserDTO userDTO = dto();
            when(userService.getUserWithImageUrl(USERNAME)).thenReturn(userDTO);

            assertThat(authService.getProfile(USERNAME)).isSameAs(userDTO);
        }
    }

    @Nested
    @DisplayName("checkAuthorization(UUID, ...)")
    class CheckAuthorizationById {

        private final UUID ownerId = UUID.randomUUID();

        @Test
        @DisplayName("owner passes")
        void owner() {
            when(userService.getUsernameById(ownerId)).thenReturn(USERNAME);

            assertThatCode(() -> authService.checkAuthorization(ownerId, USERNAME, "no"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("other user is forbidden with the given message")
        void otherUser() {
            when(userService.getUsernameById(ownerId)).thenReturn(USERNAME);

            assertThatThrownBy(() -> authService.checkAuthorization(ownerId, "intruder", "not yours"))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("not yours")
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("null current username is forbidden")
        void nullUsername() {
            when(userService.getUsernameById(ownerId)).thenReturn(USERNAME);

            assertThatThrownBy(() -> authService.checkAuthorization(ownerId, null, "no"))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("unknown owner propagates UserException")
        void unknownOwner() {
            when(userService.getUsernameById(ownerId)).thenThrow(UserException.notFound(ownerId));

            assertThatThrownBy(() -> authService.checkAuthorization(ownerId, USERNAME, "no"))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("checkAuthorization(User, ...)")
    class CheckAuthorizationByEntity {

        @Test
        @DisplayName("owner passes")
        void owner() {
            assertThatCode(() -> authService.checkAuthorization(user, USERNAME, "no"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("other user is forbidden")
        void otherUser() {
            assertThatThrownBy(() -> authService.checkAuthorization(user, "intruder", "not yours"))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("not yours");
        }

        @Test
        @DisplayName("null current username is forbidden")
        void nullUsername() {
            assertThatThrownBy(() -> authService.checkAuthorization(user, null, "no"))
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("null owner is forbidden")
        void nullOwner() {
            assertThatThrownBy(() -> authService.checkAuthorization((User) null, USERNAME, "no"))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentUsername")
    class GetCurrentUsername {

        @Test
        @DisplayName("authenticated caller returns the principal name")
        void authenticated() {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));

            assertThat(authService.getCurrentUsername()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("anonymous caller returns null")
        void anonymous() {
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            assertThat(authService.getCurrentUsername()).isNull();
        }

        @Test
        @DisplayName("no authentication returns null")
        void none() {
            assertThat(authService.getCurrentUsername()).isNull();
        }
    }
}
