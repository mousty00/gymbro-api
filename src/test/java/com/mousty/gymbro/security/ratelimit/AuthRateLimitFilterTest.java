package com.mousty.gymbro.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    private int call(String path, String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + path);
        request.setContextPath("/api");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void blocksAfterCapacityAndIgnoresSpoofedForwardedFor() throws Exception {
        for (int i = 1; i <= 20; i++) {
            // a rotating spoofed header must not give each request a fresh bucket
            assertEquals(200, call("/auth/login", "198.51.100.7", "203.0.113." + i), "request " + i);
        }
        assertEquals(429, call("/auth/login", "198.51.100.7", "203.0.113.99"));

        // other clients and non-auth paths are unaffected
        assertEquals(200, call("/auth/login", "198.51.100.8", null));
        assertEquals(200, call("/workouts", "198.51.100.7", null));
    }
}
