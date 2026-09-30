package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends GymBroException {

    private AuthException(String message, HttpStatus status) {
        super(message, status);
    }

    public static AuthException invalidOtp() {
        return new AuthException("Invalid OTP", HttpStatus.BAD_REQUEST);
    }

    public static AuthException expiredOtp() {
        return new AuthException("OTP has expired", HttpStatus.BAD_REQUEST);
    }

    public static AuthException alreadyVerified() {
        return new AuthException("User is already verified", HttpStatus.BAD_REQUEST);
    }

    public static AuthException notFound(String email) {
        return new AuthException("User not found with email: " + email, HttpStatus.NOT_FOUND);
    }

    public static AuthException forbidden(String message) {
        return new AuthException(message, HttpStatus.FORBIDDEN);
    }

    public static AuthException accountLocked(long retryAfterSeconds) {
        return new AuthException(
                "Too many failed attempts. Try again in " + retryAfterSeconds + " seconds",
                HttpStatus.TOO_MANY_REQUESTS);
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
    }
}
