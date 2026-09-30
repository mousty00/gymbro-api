package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Workout groups: alice owns the group, bob gets invited, carol stays outside. */
class GroupIntegrationTest extends IntegrationTestBase {

    private TestUser alice;
    private TestUser bob;
    private TestUser carol;
    private UUID workout;
    private UUID group;

    @BeforeEach
    void aliceCreatesGroup() throws Exception {
        alice = signupAndVerify("alice");
        bob = signupAndVerify("bob");
        carol = signupAndVerify("carol");
        workout = createWorkout(alice, true);
        final JsonNode data = gqlData(alice.token(), """
                mutation { createWorkoutGroup(request: {name: "Morning crew", workoutId: "%s", scheduledFor: "2026-10-01T07:00:00Z"}) {
                    result { id createdBy { username } } } }
                """.formatted(workout));
        group = UUID.fromString(data.at("/createWorkoutGroup/result/id").asText());
    }

    private JsonNode invite(TestUser inviter, TestUser invited) throws Exception {
        return gql(inviter.token(), """
                mutation { addGroupMember(request: {groupId: "%s", invitedUsername: "%s"}) { result { id status } } }
                """.formatted(group, invited.username()));
    }

    /** alice invites bob and bob accepts; returns the membership id. */
    private UUID bobJoins() throws Exception {
        final UUID member = UUID.fromString(invite(alice, bob).at("/data/addGroupMember/result/id").asText());
        gqlData(bob.token(), "mutation { acceptGroupInvitation(id: \"%s\") { message } }".formatted(member));
        return member;
    }

    private JsonNode logHistory(TestUser user) throws Exception {
        return gql(user.token(), """
                mutation { createWorkoutHistory(request: {workoutId: "%s", groupId: "%s", startedAt: "2026-09-30T10:00:00Z", notes: "done"}) {
                    result { id groupId } } }
                """.formatted(workout, group));
    }

    private int historiesOf(TestUser user) {
        return jdbc.queryForObject("select count(*) from workout_history where user_id = ? and group_id = ?",
                Integer.class, user.id(), group);
    }

    @Test
    @DisplayName("the group creator comes from the token")
    void creatorFromToken() {
        assertThat(jdbc.queryForObject("select created_by from workout_group where id = ?", UUID.class, group)).isEqualTo(alice.id());
    }

    @Test
    @DisplayName("an outsider gets not found on the group and its members")
    void outsiderSeesNothing() throws Exception {
        assertThat(errorType(gql(bob.token(), "{ workoutGroup(id: \"%s\") { id } }".formatted(group)))).isEqualTo("NOT_FOUND");
        assertThat(errorType(gql(bob.token(), "{ groupMembers(id: \"%s\") { results { id } } }".formatted(group)))).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("only the creator can invite")
    void outsiderCannotInvite() throws Exception {
        assertThat(errorType(invite(carol, bob))).isEqualTo("PERMISSION_DENIED");
        assertThat(jdbc.queryForObject("select count(*) from group_member where group_id = ?", Integer.class, group)).isZero();
    }

    @Test
    @DisplayName("invite flow: bob sees the invite, accepts, then sees the group and its members")
    void inviteFlow() throws Exception {
        final JsonNode invited = invite(alice, bob);
        final String member = invited.at("/data/addGroupMember/result/id").asText();
        assertThat(invited.at("/data/addGroupMember/result/status").asText()).isEqualTo("invited");

        final JsonNode mine = gqlData(bob.token(), "{ groupMembers { results { id workoutGroupId status } } }").at("/groupMembers/results");
        assertThat(each(mine, "id")).containsExactly(member);
        assertThat(mine.at("/0/status").asText()).isEqualTo("invited");

        gqlData(bob.token(), "mutation { acceptGroupInvitation(id: \"%s\") { message } }".formatted(member));
        assertThat(jdbc.queryForObject("select status from group_member where id = ?", String.class, UUID.fromString(member)))
                .isEqualTo("accepted");

        assertThat(gqlData(bob.token(), "{ workoutGroup(id: \"%s\") { name } }".formatted(group)).at("/workoutGroup/name").asText())
                .isEqualTo("Morning crew");
        final JsonNode members = gqlData(bob.token(), "{ groupMembers(id: \"%s\") { results { user { username } } } }".formatted(group));
        assertThat(members.at("/groupMembers/results/0/user/username").asText()).isEqualTo(bob.username());
    }

    @Test
    @DisplayName("an accepted member cannot 'reject' the invitation")
    void acceptedMemberCannotReject() throws Exception {
        final UUID member = bobJoins();

        final JsonNode response = gql(bob.token(), "mutation { rejectGroupInvitation(id: \"%s\") { message } }".formatted(member));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(jdbc.queryForObject("select count(*) from group_member where id = ?", Integer.class, member)).isEqualTo(1);
    }

    @Test
    @DisplayName("a non-member cannot log a workout into the group")
    void nonMemberCannotLogHistory() throws Exception {
        assertThat(errorType(logHistory(carol))).isEqualTo("NOT_FOUND");
        assertThat(historiesOf(carol)).isZero();
    }

    @Test
    @DisplayName("a member can log a workout into the group")
    void memberLogsHistory() throws Exception {
        bobJoins();

        final JsonNode response = logHistory(bob);

        assertThat(response.at("/data/createWorkoutHistory/result/groupId").asText()).isEqualTo(group.toString());
        assertThat(historiesOf(bob)).isEqualTo(1);
    }

    @Test
    @DisplayName("group history is readable by the owner and members, not by outsiders")
    void groupHistoryVisibility() throws Exception {
        bobJoins();
        final String historyId = logHistory(bob).at("/data/createWorkoutHistory/result/id").asText();
        final String query = "{ GroupWorkoutHistories(groupId: \"%s\") { results { id } } }".formatted(group);

        assertThat(each(gqlData(alice.token(), query).at("/GroupWorkoutHistories/results"), "id")).containsExactly(historyId);
        assertThat(each(gqlData(bob.token(), query).at("/GroupWorkoutHistories/results"), "id")).containsExactly(historyId);
        assertThat(errorType(gql(carol.token(), query))).isEqualTo("NOT_FOUND");
    }
}
