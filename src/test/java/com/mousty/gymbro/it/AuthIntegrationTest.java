package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    private ResultActions login(String username, String password) throws Exception {
        return postJson("/auth/login", Map.of("username", username, "password", password), null);
    }

    private ResultActions resetPassword(String email, String otp, String newPassword) throws Exception {
        return postJson("/auth/reset-password", Map.of("email", email, "otp", otp, "newPassword", newPassword), null);
    }

    private void requestResetOtp(String email) throws Exception {
        rest(post("/auth/send-reset-otp").param("email", email), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("login is refused (403) until the email is verified, then succeeds")
    void loginRequiresVerifiedEmail() throws Exception {
        final String username = signup("unverified");

        login(username, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(startsWith("Email not verified")));

        postJson("/auth/verify-otp", Map.of("username", username, "otp", lastVerifyOtp(emailOf(username))), null)
                .andExpect(status().isOk());

        login(username, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
        assertThat(jdbc.queryForObject("select is_account_verified from \"user\" where username = ?", Boolean.class, username))
                .isTrue();
    }

    @Test
    @DisplayName("unknown user and wrong password give the same 401 message")
    void noUsernameEnumerationOnLogin() throws Exception {
        final TestUser user = signupAndVerify("enum");

        login(unique("nobody"), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        login(user.username(), "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("send-otp for an unknown email answers 200 generically and sends nothing")
    void sendOtpUnknownEmailIsSilent() throws Exception {
        rest(post("/auth/send-otp").param("email", unique("ghost") + "@example.com"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(startsWith("If the account exists")));

        verify(emailService, never()).sendOtpEmail(any(), any(), any());
    }

    @Test
    @DisplayName("stored OTPs are hashed, never the emailed code")
    void storedOtpIsHashed() throws Exception {
        final TestUser user = signupAndVerify("hash");
        requestResetOtp(user.email());

        final String emailed = lastResetOtp(user.email());
        final String stored = jdbc.queryForObject("select reset_otp from \"user\" where id = ?", String.class, user.id());

        assertThat(emailed).matches("\\d{6}");
        assertThat(stored).isNotEqualTo(emailed).doesNotMatch("\\d{6}");
    }

    @Test
    @DisplayName("password reset end to end; the OTP is single use")
    void passwordResetEndToEnd() throws Exception {
        final TestUser user = signupAndVerify("reset");
        requestResetOtp(user.email());
        final String otp = lastResetOtp(user.email());

        resetPassword(user.email(), otp, "new-password-1").andExpect(status().isOk());

        login(user.username(), "new-password-1").andExpect(status().isOk());
        login(user.username(), PASSWORD).andExpect(status().isUnauthorized());
        resetPassword(user.email(), otp, "new-password-2").andExpect(status().isBadRequest());
        login(user.username(), "new-password-1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("OTP lockout is committed across requests: the 6th attempt is locked out")
    void otpLockoutPersists() throws Exception {
        final TestUser user = signupAndVerify("lockout");
        requestResetOtp(user.email());
        final String realOtp = lastResetOtp(user.email());
        final String wrongOtp = realOtp.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            resetPassword(user.email(), wrongOtp, "new-password-1")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid OTP"));
        }
        resetPassword(user.email(), realOtp, "new-password-1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(startsWith("Too many failed attempts")));

        final Map<String, Object> row = jdbc.queryForMap(
                "select otp_failed_attempts, otp_locked_until, reset_otp from \"user\" where id = ?", user.id());
        assertThat(row.get("otp_failed_attempts")).isEqualTo(5);
        assertThat(row.get("otp_locked_until")).isNotNull();
        assertThat(row.get("reset_otp")).isNull();
    }

    @Test
    @DisplayName("GraphQL alias batching of 300 resetPassword guesses is rejected by the complexity limit")
    void aliasBatchingBlocked() throws Exception {
        final StringBuilder mutation = new StringBuilder("mutation {");
        for (int i = 0; i < 300; i++) {
            mutation.append(" a%d: resetPassword(request: {email: \"x@example.com\", otp: \"%06d\", newPassword: \"password123\"}) { message }"
                    .formatted(i, i));
        }
        mutation.append(" }");

        final JsonNode response = gql(null, mutation.toString());

        assertThat(response.path("data").path("a0").isMissingNode() || response.path("data").isNull()).isTrue();
        assertThat(errorMessage(response)).contains("maximum query complexity exceeded");
    }

    @Test
    @DisplayName("anonymous /auth/profile is 401")
    void profileRequiresAuthentication() throws Exception {
        rest(get("/auth/profile"), null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("is-authenticated is false without a token and true with one")
    void isAuthenticated() throws Exception {
        final TestUser user = signupAndVerify("isauth");

        rest(get("/auth/is-authenticated"), null).andExpect(status().isOk()).andExpect(content().string("false"));
        rest(get("/auth/is-authenticated"), user.token()).andExpect(status().isOk()).andExpect(content().string("true"));
    }

    @Test
    @DisplayName("refresh token rotates: the old one can't be reused")
    void refreshTokenRotates() throws Exception {
        final TestUser user = signupAndVerify("refresh");

        final JsonNode refreshed = body(postJson("/auth/refresh", Map.of("refreshToken", user.refreshToken()), null)
                .andExpect(status().isOk()));
        assertThat(refreshed.get("refreshToken").asText()).isNotBlank().isNotEqualTo(user.refreshToken());

        postJson("/auth/refresh", Map.of("refreshToken", user.refreshToken()), null)
                .andExpect(status().isUnauthorized());
        postJson("/auth/refresh", Map.of("refreshToken", refreshed.get("refreshToken").asText()), null)
                .andExpect(status().isOk());
    }
}
