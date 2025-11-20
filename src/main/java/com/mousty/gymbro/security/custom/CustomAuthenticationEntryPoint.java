package com.mousty.gymbro.security.custom;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mousty.gymbro.response.MessageResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint, AuthenticationFailureHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        handleAuthenticationException(response, authException);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        handleAuthenticationException(response, exception);
    }

    private void handleAuthenticationException(HttpServletResponse response,
                                               AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String errorMessage = "Authentication failed";

        if (exception instanceof UsernameNotFoundException) {
            errorMessage = "Username not found";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "Invalid password";
        } else if (exception instanceof InsufficientAuthenticationException) {
            errorMessage = "Authentication required";
        }

        MessageResponse messageResponse = MessageResponse.builder()
                .message(errorMessage)
                .build();

        objectMapper.writeValue(response.getWriter(), messageResponse);
    }
}