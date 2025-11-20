package com.mousty.gymbro.config;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.GraphQLContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Locale;
import java.util.Map;

@Component
public class UploadScalar implements Coercing<MultipartFile, Void> {

    @Override
    public Void serialize(@NotNull Object dataFetcherResult,
                          @NotNull GraphQLContext graphQLContext,
                          @NotNull Locale locale) throws CoercingSerializeException {
        try {
            throw new CoercingSerializeException("Upload serialization not supported");
        } catch (Exception e) {
            throw new CoercingSerializeException("Failed to serialize upload", e);
        }
    }

    @Override
    public MultipartFile parseValue(@NotNull Object input,
                                    @NotNull GraphQLContext graphQLContext,
                                    @NotNull Locale locale) throws CoercingParseValueException {
        try {
            if (input instanceof final Map<?, ?> map) {
                if (map.containsKey("file") && map.get("file") instanceof MultipartFile) {
                    return (MultipartFile) map.get("file");
                }
            }
            throw new CoercingParseValueException("Expected a file upload");
        } catch (Exception e) {
            throw new CoercingParseValueException("Failed to parse upload value", e);
        }
    }

    public static GraphQLScalarType create() {
        return GraphQLScalarType.newScalar()
                .name("MultiPartFile")
                .description("A file upload scalar")
                .coercing(new UploadScalar())
                .build();
    }
}