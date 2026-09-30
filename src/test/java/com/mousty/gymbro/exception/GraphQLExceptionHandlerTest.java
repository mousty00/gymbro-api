package com.mousty.gymbro.exception;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.ResultPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GraphQLExceptionHandlerTest {

    private final GraphQLExceptionHandler handler = new GraphQLExceptionHandler();

    private GraphQLError handle(Throwable ex) throws Exception {
        DataFetcherExceptionHandlerParameters params = mock(DataFetcherExceptionHandlerParameters.class);
        when(params.getException()).thenReturn(ex);
        when(params.getPath()).thenReturn(ResultPath.parse("/workout"));
        return handler.handleException(params).get().getErrors().getFirst();
    }

    private static String errorType(GraphQLError error) {
        return String.valueOf(error.getExtensions().get("errorType"));
    }

    @Test
    @DisplayName("domain exceptions keep their message and map their HTTP status to an errorType")
    void domainException() throws Exception {
        UUID id = UUID.randomUUID();
        GraphQLError notFound = handle(WorkoutException.notFound(id));
        assertThat(notFound.getMessage()).isEqualTo("Workout not found: " + id);
        assertThat(errorType(notFound)).isEqualTo("NOT_FOUND");

        assertThat(errorType(handle(AuthException.forbidden("no")))).isEqualTo("PERMISSION_DENIED");
        assertThat(errorType(handle(AuthException.invalidRefreshToken()))).isEqualTo("UNAUTHENTICATED");
        assertThat(errorType(handle(AuthException.accountLocked(10)))).isEqualTo("UNAVAILABLE");
        assertThat(errorType(handle(AuthException.invalidOtp()))).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("security exceptions get generic, class-free messages")
    void securityExceptions() throws Exception {
        GraphQLError denied = handle(new AccessDeniedException("internal detail"));
        assertThat(denied.getMessage()).isEqualTo("Access denied");
        assertThat(errorType(denied)).isEqualTo("PERMISSION_DENIED");

        assertThat(errorType(handle(new BadCredentialsException("Invalid username or password")))).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    @DisplayName("unexpected exceptions return a generic message: no class names or internals leak")
    void unexpected() throws Exception {
        // IllegalStateException isn't mapped, so it falls into the generic branch
        GraphQLError error = handle(new IllegalStateException("could not execute statement [ERROR: null value in column ...]"));

        assertThat(error.getMessage()).isEqualTo("An unexpected error occurred").doesNotContain("column");
        assertThat(errorType(error)).isEqualTo("INTERNAL");
    }
}
