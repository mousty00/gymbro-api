package com.mousty.gymbro.config;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsRuntimeWiring;
import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@DgsComponent
public class GraphQLScalarsConfig {

    @DgsRuntimeWiring
    public RuntimeWiring.Builder addScalars(RuntimeWiring.Builder builder) {
        return builder
                .scalar(ExtendedScalars.UUID)
                .scalar(UploadScalar.create())
                .scalar(ExtendedScalars.Json)
                .scalar(ExtendedScalars.Date)
                .scalar(ExtendedScalars.Time)
                .scalar(ExtendedScalars.DateTime)
                .scalar(INSTANT);
    }

    // ISO-8601 instant (e.g. 2026-09-30T10:00:00Z) <-> java.time.Instant. DGS can't convert
    // String inputs to Instant on its own, so input fields use this scalar.
    private static final GraphQLScalarType INSTANT = GraphQLScalarType.newScalar()
            .name("Instant")
            .description("ISO-8601 UTC timestamp, e.g. 2026-09-30T10:00:00Z")
            .coercing(new Coercing<Instant, String>() {
                @Override
                public String serialize(Object value, GraphQLContext context, Locale locale) {
                    return value.toString();
                }

                @Override
                public Instant parseValue(Object input, GraphQLContext context, Locale locale) {
                    try {
                        return Instant.parse(input.toString());
                    } catch (DateTimeParseException e) {
                        throw new CoercingParseValueException("Invalid Instant: " + input);
                    }
                }

                @Override
                public Instant parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
                    if (input instanceof StringValue s) {
                        try {
                            return Instant.parse(s.getValue());
                        } catch (DateTimeParseException e) {
                            throw new CoercingParseLiteralException("Invalid Instant: " + s.getValue());
                        }
                    }
                    throw new CoercingParseLiteralException("Instant must be a string");
                }
            })
            .build();
}

