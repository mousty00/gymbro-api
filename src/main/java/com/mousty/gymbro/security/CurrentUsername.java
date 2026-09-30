package com.mousty.gymbro.security;

import org.springframework.security.core.annotation.CurrentSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the username of the caller authenticated by JwtFilter (from the bearer token).
 * Resolves to null for anonymous callers — never to Spring's "anonymousUser" placeholder,
 * which a real account could register as. Services must treat the value as the only
 * source of caller identity; never trust user ids sent in request bodies.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@CurrentSecurityContext(expression =
        "authentication == null || authentication instanceof T(org.springframework.security.authentication.AnonymousAuthenticationToken)"
                + " ? null : authentication.name")
public @interface CurrentUsername {
}
