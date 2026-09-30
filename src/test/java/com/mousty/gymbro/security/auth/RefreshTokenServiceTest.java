package com.mousty.gymbro.security.auth;

import com.mousty.gymbro.entity.RefreshToken;
import com.mousty.gymbro.entity.User;
import com.mousty.gymbro.exception.AuthException;
import com.mousty.gymbro.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long EXPIRATION_MS = 60_000L;

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", EXPIRATION_MS);
        user = User.builder().id(UUID.randomUUID()).username("mousty").build();
    }

    /** Issues a token and returns the entity that was saved for it. */
    private RefreshToken issueAndCapture(String[] rawOut) {
        rawOut[0] = service.issue(user);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("stores a hash of the token, not the raw token")
        void storesHash() {
            String[] raw = new String[1];
            RefreshToken saved = issueAndCapture(raw);

            assertThat(raw[0]).isNotBlank();
            assertThat(saved.getTokenHash()).isNotBlank().isNotEqualTo(raw[0]);
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getRevoked()).isFalse();
        }

        @Test
        @DisplayName("expiry uses the configured duration")
        void expiry() {
            String[] raw = new String[1];
            RefreshToken saved = issueAndCapture(raw);

            assertThat(saved.getExpiresAt().toEpochMilli())
                    .isCloseTo(Instant.now().plusMillis(EXPIRATION_MS).toEpochMilli(), within(5000L));
        }

        @Test
        @DisplayName("each call returns a different token and hash")
        void unique() {
            String first = service.issue(user);
            String second = service.issue(user);

            assertThat(first).isNotEqualTo(second);
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getTokenHash())
                    .isNotEqualTo(captor.getAllValues().get(1).getTokenHash());
        }
    }

    @Nested
    @DisplayName("validateAndRevoke")
    class ValidateAndRevoke {

        private String raw;
        private RefreshToken stored;

        @BeforeEach
        void issueToken() {
            String[] out = new String[1];
            stored = issueAndCapture(out);
            raw = out[0];
        }

        @Test
        @DisplayName("valid token is looked up by hash, revoked and returns its user")
        void valid() {
            when(repository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

            User result = service.validateAndRevoke(raw);

            assertThat(result).isSameAs(user);
            assertThat(stored.getRevoked()).isTrue();
            verify(repository, times(2)).save(stored);
        }

        @Test
        @DisplayName("unknown token is rejected")
        void unknown() {
            when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validateAndRevoke("not-a-token"))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid or expired refresh token")
                    .extracting(e -> ((AuthException) e).getStatus())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("already revoked token is rejected (no replay)")
        void revoked() {
            stored.setRevoked(true);
            when(repository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> service.validateAndRevoke(raw))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("expired token is rejected and not saved again")
        void expired() {
            stored.setExpiresAt(Instant.now().minusSeconds(1));
            when(repository.findByTokenHash(stored.getTokenHash())).thenReturn(Optional.of(stored));

            assertThatThrownBy(() -> service.validateAndRevoke(raw))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid or expired refresh token");
            assertThat(stored.getRevoked()).isFalse();
            verify(repository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("revokeAllForUser")
    class RevokeAll {

        @Test
        @DisplayName("delegates to the repository bulk update")
        void delegates() {
            service.revokeAllForUser(user);

            verify(repository).revokeAllForUser(user);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("purgeExpiredAndRevoked")
    class Purge {

        @Test
        @DisplayName("bulk-deletes tokens expired before now (and revoked ones)")
        void deletesExpired() {
            Instant before = Instant.now();

            service.purgeExpiredAndRevoked();

            ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
            verify(repository).deleteExpiredOrRevoked(cutoff.capture());
            assertThat(cutoff.getValue()).isBetween(before, Instant.now());
        }
    }
}
