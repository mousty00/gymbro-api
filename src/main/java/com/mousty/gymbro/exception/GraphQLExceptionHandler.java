package com.mousty.gymbro.exception;

import com.netflix.graphql.types.errors.ErrorType;
import com.netflix.graphql.types.errors.TypedGraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * GraphQL counterpart of GlobalExceptionHandler: domain errors keep their message and a
 * matching errorType; anything unexpected is logged and returned as a generic message, so
 * exception class names and internals never reach the client.
 */
@Slf4j
@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters params) {
        final Throwable ex = params.getException();
        final TypedGraphQLError.Builder error = TypedGraphQLError.newBuilder()
                .path(params.getPath())
                .location(params.getSourceLocation());

        switch (ex) {
            case GymBroException e -> error.errorType(errorType(e)).message(e.getMessage());
            case AccessDeniedException e -> error.errorType(ErrorType.PERMISSION_DENIED).message("Access denied");
            case AuthenticationException e -> error.errorType(ErrorType.UNAUTHENTICATED).message(e.getMessage());
            case ConstraintViolationException e -> error.errorType(ErrorType.BAD_REQUEST).message(e.getMessage());
            case IllegalArgumentException e -> error.errorType(ErrorType.BAD_REQUEST).message(e.getMessage());
            default -> {
                log.error("Unhandled GraphQL exception at {}", params.getPath(), ex);
                error.errorType(ErrorType.INTERNAL).message("An unexpected error occurred");
            }
        }

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult().error(error.build()).build());
    }

    private static ErrorType errorType(GymBroException e) {
        return switch (e.getStatus()) {
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            case FORBIDDEN -> ErrorType.PERMISSION_DENIED;
            case UNAUTHORIZED -> ErrorType.UNAUTHENTICATED;
            case TOO_MANY_REQUESTS -> ErrorType.UNAVAILABLE;
            case CONFLICT -> ErrorType.FAILED_PRECONDITION;
            default -> e.getStatus().is4xxClientError() ? ErrorType.BAD_REQUEST : ErrorType.INTERNAL;
        };
    }
}
