package com.mousty.gymbro.security;

import com.mousty.gymbro.security.auth.AuthService;
import com.netflix.graphql.dgs.internal.method.ArgumentResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;

/**
 * Makes {@link CurrentUsername} work on DGS data fetchers — DGS doesn't evaluate the
 * Spring Security annotation itself (it silently injects null). REST gets it from Spring MVC.
 */
@Component
@RequiredArgsConstructor
public class CurrentUsernameArgumentResolver implements ArgumentResolver {

    private final AuthService authService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUsername.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, DataFetchingEnvironment environment) {
        return authService.getCurrentUsername();
    }
}
