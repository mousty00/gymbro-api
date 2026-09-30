package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Error responses keep a proper status and leak no internals. */
class ApiErrorsIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("malformed JSON on REST is a 400")
    void malformedJson() throws Exception {
        rest(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\": "), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    @DisplayName("the wrong HTTP method is a 405")
    void wrongMethod() throws Exception {
        rest(get("/auth/login"), null).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("GraphQL domain errors expose no Java class names")
    void domainErrorsHaveNoClassNames() throws Exception {
        final TestUser user = signupAndVerify("errors");

        final JsonNode response = gql(user.token(), "{ workout(id: \"%s\") { id } }".formatted(UUID.randomUUID()));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(errorMessage(response)).startsWith("Workout not found").doesNotContain("com.mousty", "Exception");
    }

    @Test
    @DisplayName("unexpected GraphQL errors return a generic message")
    void unexpectedErrorsAreGeneric() throws Exception {
        final TestUser user = signupAndVerify("errors");
        final UUID workout = createWorkout(user, false);
        final String exercise = gqlData(user.token(), """
                mutation { createExercise(request: {name: "Row", muscleGroup: "back", isPublic: false}) { result { id } } }
                """).at("/createExercise/result/id").asText();

        // sets and position are NOT NULL in the DB but optional in the schema: a constraint violation, not a domain error
        final JsonNode response = gql(user.token(), """
                mutation { createWorkoutExercise(request: {workoutId: "%s", exerciseId: "%s"}) { result { id } } }
                """.formatted(workout, exercise));

        assertThat(errorType(response)).isEqualTo("INTERNAL");
        assertThat(errorMessage(response)).isEqualTo("An unexpected error occurred");
        assertThat(response.toString()).doesNotContain("com.mousty", "Exception", "constraint");
    }
}
