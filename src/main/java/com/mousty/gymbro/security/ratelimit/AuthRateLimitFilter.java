package com.mousty.gymbro.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-IP rate limit on /auth/** — a second layer on top of the transport-agnostic
 * account lockout in AuthService, meant to slow down scripted credential/OTP abuse
 * before it ever reaches the service layer.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 20;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_IPS = 10_000;

    // ponytail: in-memory LRU, per instance. Evicting the oldest IP only resets its budget.
    // Move to Redis (bucket4j-redis) if the API ever runs on more than one instance.
    private final Map<String, Bucket> buckets = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                    return size() > MAX_TRACKED_IPS;
                }
            });

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        if (!isAuthPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests, please try again later\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/auth");
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD)))
                .build();
    }

    // Tomcat's RemoteIpValve (server.forward-headers-strategy=native) already resolves the real
    // client IP from X-Forwarded-For, trusting only internal proxies (Traefik). Reading the header
    // here directly would let any client spoof it and dodge the limit.
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
