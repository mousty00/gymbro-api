package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** IDOR checks: bob must never read or change alice's resources. */
class AuthorizationIntegrationTest extends IntegrationTestBase {

    private TestUser alice;
    private TestUser bob;

    @BeforeEach
    void users() throws Exception {
        alice = signupAndVerify("alice");
        bob = signupAndVerify("bob");
    }

    private UUID createExercise(TestUser owner) throws Exception {
        final JsonNode data = gqlData(owner.token(), """
                mutation { createExercise(request: {name: "Squat", muscleGroup: "legs", isPublic: true}) { result { id } } }
                """);
        return UUID.fromString(data.at("/createExercise/result/id").asText());
    }

    private String workoutName(UUID id) {
        return jdbc.queryForObject("select name from workout where id = ?", String.class, id);
    }

    // ---------- workouts ----------

    @Test
    @DisplayName("bob cannot read alice's private workout")
    void cannotReadOthersPrivateWorkout() throws Exception {
        final UUID workout = createWorkout(alice, false);

        final JsonNode response = gql(bob.token(), "{ workout(id: \"%s\") { id name } }".formatted(workout));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(response.at("/data/workout").isNull()).isTrue();
    }

    @Test
    @DisplayName("bob cannot update alice's workout; alice can")
    void onlyOwnerUpdatesWorkout() throws Exception {
        final UUID workout = createWorkout(alice, false);
        final String update = """
                mutation { updateWorkout(request: {id: "%s", name: "%s", isPublic: false, dayOfWeek: [2]}) { message } }
                """;

        assertThat(errorType(gql(bob.token(), update.formatted(workout, "hacked")))).isEqualTo("NOT_FOUND");
        assertThat(workoutName(workout)).isEqualTo("Leg day");

        gqlData(alice.token(), update.formatted(workout, "Push day"));
        assertThat(workoutName(workout)).isEqualTo("Push day");
    }

    @Test
    @DisplayName("bob cannot delete alice's workout")
    void cannotDeleteOthersWorkout() throws Exception {
        final UUID workout = createWorkout(alice, false);

        final JsonNode response = gql(bob.token(), "mutation { deleteWorkout(id: \"%s\") { message } }".formatted(workout));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(jdbc.queryForObject("select count(*) from workout where id = ?", Integer.class, workout)).isEqualTo(1);
    }

    @Test
    @DisplayName("alice's private workout is not in bob's REST workout list")
    void privateWorkoutNotListedForOthers() throws Exception {
        final UUID privateWorkout = createWorkout(alice, false);
        final UUID bobsWorkout = createWorkout(bob, false);

        final JsonNode page = body(rest(get("/workouts").param("size", "100"), bob.token()).andExpect(status().isOk()));

        assertThat(each(page.get("results"), "id"))
                .contains(bobsWorkout.toString())
                .doesNotContain(privateWorkout.toString());
    }

    @Test
    @DisplayName("createWorkout takes the owner from the token")
    void createWorkoutOwnerFromToken() throws Exception {
        final UUID workout = createWorkout(bob, true);

        assertThat(jdbc.queryForObject("select user_id from workout where id = ?", UUID.class, workout)).isEqualTo(bob.id());
    }

    @Test
    @DisplayName("createWorkout input has no userId: sending one is a schema validation error")
    void createWorkoutRejectsUserId() throws Exception {
        final JsonNode response = gql(bob.token(), """
                mutation { createWorkout(request: {name: "x", isPublic: true, dayOfWeek: [1], userId: "%s"}) { result { id } } }
                """.formatted(alice.id()));

        assertThat(response.at("/errors/0/extensions/classification").asText()).isEqualTo("ValidationError");
        assertThat(jdbc.queryForObject("select count(*) from workout where user_id = ?", Integer.class, alice.id())).isZero();
    }

    // ---------- workout exercises ----------

    @Test
    @DisplayName("bob cannot add an exercise into alice's workout")
    void cannotAddExerciseToOthersWorkout() throws Exception {
        final UUID workout = createWorkout(alice, false);
        final UUID exercise = createExercise(bob);

        final JsonNode response = gql(bob.token(), """
                mutation { createWorkoutExercise(request: {workoutId: "%s", exerciseId: "%s", sets: 3, reps: 10, position: 1}) { result { id } } }
                """.formatted(workout, exercise));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(jdbc.queryForObject("select count(*) from workout_exercise where workout_id = ?", Integer.class, workout)).isZero();
    }

    @Test
    @DisplayName("workoutExercises lists only the caller's rows")
    void workoutExercisesOnlyOwn() throws Exception {
        final UUID workout = createWorkout(alice, true);
        final UUID exercise = createExercise(alice);
        final JsonNode created = gqlData(alice.token(), """
                mutation { createWorkoutExercise(request: {workoutId: "%s", exerciseId: "%s", sets: 3, reps: 10, position: 1}) { result { id } } }
                """.formatted(workout, exercise));
        final String rowId = created.at("/createWorkoutExercise/result/id").asText();

        final JsonNode alices = gqlData(alice.token(), "{ workoutExercises(size: 100) { results { id } } }");
        final JsonNode bobs = gqlData(bob.token(), "{ workoutExercises(size: 100) { results { id } } }");

        assertThat(each(alices.at("/workoutExercises/results"), "id")).containsExactly(rowId);
        assertThat(each(bobs.at("/workoutExercises/results"), "id")).isEmpty();
    }

    // ---------- exercises ----------

    @Test
    @DisplayName("bob cannot update alice's exercise and its owner stays alice")
    void cannotUpdateOthersExercise() throws Exception {
        final UUID exercise = createExercise(alice);

        final JsonNode response = gql(bob.token(), """
                mutation { updateExercise(request: {id: "%s", name: "mine now", muscleGroup: "arms", isPublic: false}) { message } }
                """.formatted(exercise));

        assertThat(errorType(response)).isEqualTo("PERMISSION_DENIED");
        assertThat(errorMessage(response)).isEqualTo("User not authorized to update exercise");
        assertThat(jdbc.queryForMap("select name, created_by from exercise where id = ?", exercise))
                .containsEntry("name", "Squat")
                .containsEntry("created_by", alice.id());
    }

    // ---------- users ----------

    @Test
    @DisplayName("deleteUser no longer accepts an id argument")
    void deleteUserRejectsIdArgument() throws Exception {
        final JsonNode response = gql(bob.token(), "mutation { deleteUser(id: \"%s\") { message } }".formatted(alice.id()));

        assertThat(response.at("/errors/0/extensions/classification").asText()).isEqualTo("ValidationError");
        assertThat(userExists(alice)).isTrue();
    }

    @Test
    @DisplayName("REST DELETE /users/{id} is not a route (405)")
    void restDeleteUserByIdIsNotAllowed() throws Exception {
        rest(delete("/users/" + alice.id()), bob.token()).andExpect(status().isMethodNotAllowed());

        assertThat(userExists(alice)).isTrue();
    }

    @Test
    @DisplayName("deleteUser deletes only the caller")
    void deleteUserDeletesOnlyCaller() throws Exception {
        gqlData(bob.token(), "mutation { deleteUser { message } }");

        assertThat(userExists(bob)).isFalse();
        assertThat(userExists(alice)).isTrue();
    }

    @Test
    @DisplayName("users list hides email, last name and birth date of everyone")
    void usersListHidesPii() throws Exception {
        final JsonNode results = gqlData(bob.token(), "{ users(size: 100) { results { username email lastName birthDate } } }")
                .at("/users/results");

        assertThat(results).isNotEmpty();
        results.forEach(user -> {
            assertThat(user.get("email").isNull()).as("email of %s", user.get("username")).isTrue();
            assertThat(user.get("lastName").isNull()).isTrue();
            assertThat(user.get("birthDate").isNull()).isTrue();
        });
    }

    @Test
    @DisplayName("the owner's own /auth/profile includes her email")
    void ownProfileHasEmail() throws Exception {
        rest(get("/auth/profile"), alice.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(alice.email()));
    }

    @Test
    @DisplayName("a page size of 100000 is capped instead of failing")
    void hugePageSizeIsCapped() throws Exception {
        final JsonNode users = gqlData(bob.token(), "{ users(size: 100000) { results { id } pageInfo { totalPages } } }")
                .get("users");

        assertThat(users.get("results").size()).isBetween(2, 100);
    }

    private boolean userExists(TestUser user) {
        return jdbc.queryForObject("select count(*) from \"user\" where id = ?", Integer.class, user.id()) == 1;
    }
}
