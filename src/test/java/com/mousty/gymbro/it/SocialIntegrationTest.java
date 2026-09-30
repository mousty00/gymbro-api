package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Comments, likes and friendships. */
class SocialIntegrationTest extends IntegrationTestBase {

    private TestUser alice;
    private TestUser bob;
    private UUID alicesPost;

    @BeforeEach
    void users() throws Exception {
        alice = signupAndVerify("alice");
        bob = signupAndVerify("bob");
        alicesPost = insertPost(alice.id(), "alice's post");
    }

    private UUID comment(TestUser author, String content) throws Exception {
        final JsonNode data = gqlData(author.token(), """
                mutation { createComment(request: {postId: "%s", content: "%s"}) { result { id user { username } } } }
                """.formatted(alicesPost, content));
        return UUID.fromString(data.at("/createComment/result/id").asText());
    }

    private UUID like(TestUser user) throws Exception {
        final JsonNode data = gqlData(user.token(), """
                mutation { createLike(request: {postId: "%s", userId: "%s"}) { id } }
                """.formatted(alicesPost, user.id()));
        return UUID.fromString(data.at("/createLike/id").asText());
    }

    private int count(String table, UUID id) {
        return jdbc.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
    }

    private UUID addFriend(TestUser from, TestUser to) throws Exception {
        gqlData(from.token(), "mutation { addFriend(friendUsername: \"%s\") { message } }".formatted(to.username()));
        return jdbc.queryForObject("select f.id from friendship f join \"user\" u on u.id = f.user_id "
                + "where u.username = ? and f.friend_id = ?", UUID.class, from.username(), to.id());
    }

    // ---------- comments ----------

    @Test
    @DisplayName("a comment's author comes from the token")
    void commentAuthorFromToken() throws Exception {
        final UUID comment = comment(bob, "nice");

        assertThat(jdbc.queryForObject("select user_id from post_comment where id = ?", UUID.class, comment)).isEqualTo(bob.id());
    }

    @Test
    @DisplayName("bob cannot edit alice's comment over REST (403)")
    void cannotEditOthersComment() throws Exception {
        final UUID comment = comment(alice, "original");
        final Map<String, Object> edit = Map.of("id", comment, "postId", alicesPost, "username", bob.username(),
                "content", "defaced", "createdAt", "2026-09-30T10:00:00Z");

        rest(put("/comments").contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(edit)), bob.token())
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("select content from post_comment where id = ?", String.class, comment)).isEqualTo("original");
    }

    @Test
    @DisplayName("alice can edit her own comment over REST")
    void ownerEditsComment() throws Exception {
        final UUID comment = comment(alice, "original");
        final Map<String, Object> edit = Map.of("id", comment, "postId", alicesPost, "username", alice.username(),
                "content", "edited", "createdAt", "2026-09-30T10:00:00Z");

        rest(put("/comments").contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(edit)), alice.token())
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select content from post_comment where id = ?", String.class, comment)).isEqualTo("edited");
    }

    @Test
    @DisplayName("bob cannot delete alice's comment; alice can")
    void onlyAuthorDeletesComment() throws Exception {
        final UUID comment = comment(alice, "mine");
        final String delete = "mutation { deleteComment(id: \"%s\") { message } }".formatted(comment);

        assertThat(errorType(gql(bob.token(), delete))).isEqualTo("PERMISSION_DENIED");
        assertThat(count("post_comment", comment)).isEqualTo(1);

        gqlData(alice.token(), delete);
        assertThat(count("post_comment", comment)).isZero();
    }

    // ---------- likes ----------

    @Test
    @DisplayName("bob cannot delete alice's like")
    void cannotDeleteOthersLike() throws Exception {
        final UUID like = like(alice);

        final JsonNode response = gql(bob.token(), "mutation { deleteLike(id: \"%s\") { message } }".formatted(like));

        assertThat(errorType(response)).isEqualTo("PERMISSION_DENIED");
        assertThat(count("post_like", like)).isEqualTo(1);
    }

    @Test
    @DisplayName("liking the same post twice is rejected")
    void duplicateLikeRejected() throws Exception {
        like(bob);

        final JsonNode response = gql(bob.token(), """
                mutation { createLike(request: {postId: "%s", userId: "%s"}) { id } }
                """.formatted(alicesPost, bob.id()));

        assertThat(errorMessage(response)).isEqualTo("User has already liked this post");
        assertThat(jdbc.queryForObject("select count(*) from post_like where post_id = ? and user_id = ?",
                Integer.class, alicesPost, bob.id())).isEqualTo(1);
    }

    // ---------- friendship ----------

    @Test
    @DisplayName("a friend request to yourself is a BAD_REQUEST")
    void selfFriendRequestRejected() throws Exception {
        final JsonNode response = gql(alice.token(), "mutation { addFriend(friendUsername: \"%s\") { message } }".formatted(alice.username()));

        assertThat(errorType(response)).isEqualTo("BAD_REQUEST");
    }

    @Test
    @DisplayName("a reverse request while one is pending is rejected as a duplicate")
    void reverseFriendRequestRejected() throws Exception {
        addFriend(alice, bob);

        final JsonNode response = gql(bob.token(), "mutation { addFriend(friendUsername: \"%s\") { message } }".formatted(alice.username()));

        assertThat(errorMessage(response)).isEqualTo("Users are already friends or a request is pending");
        assertThat(jdbc.queryForObject("select count(*) from friendship where user_id in (?, ?)", Integer.class,
                alice.id(), bob.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("friend request flow: only the recipient accepts, once; then both are friends and see posts")
    void friendRequestFlow() throws Exception {
        final UUID request = addFriend(alice, bob);

        final JsonNode requests = gqlData(bob.token(), "{ friendRequests { id user { username } } }").get("friendRequests");
        assertThat(each(requests, "id")).containsExactly(request.toString());

        final String accept = "mutation { acceptFriend(id: \"%s\") { message } }".formatted(request);
        assertThat(errorType(gql(alice.token(), accept))).isEqualTo("PERMISSION_DENIED");
        assertThat(jdbc.queryForObject("select status from friendship where id = ?", String.class, request)).isEqualTo("pending");

        gqlData(bob.token(), accept);
        assertThat(jdbc.queryForObject("select status from friendship where id = ?", String.class, request)).isEqualTo("accepted");
        assertThat(errorType(gql(bob.token(), accept))).isEqualTo("NOT_FOUND");

        final String friends = "{ friends { results { user { username } friend { username } } } }";
        assertThat(gqlData(alice.token(), friends).at("/friends/results/0/friend/username").asText()).isEqualTo(bob.username());
        assertThat(gqlData(bob.token(), friends).at("/friends/results/0/user/username").asText()).isEqualTo(alice.username());

        final JsonNode posts = gqlData(bob.token(), "{ friendsPosts { id content } }").get("friendsPosts");
        assertThat(each(posts, "id")).containsExactly(alicesPost.toString());
    }

    @Test
    @DisplayName("a third user cannot read someone else's friendship")
    void thirdPartyCannotReadFriendship() throws Exception {
        final UUID request = addFriend(alice, bob);
        final TestUser carol = signupAndVerify("carol");

        final JsonNode response = gql(carol.token(), "{ friend(id: \"%s\") { id } }".formatted(request));

        assertThat(errorType(response)).isEqualTo("NOT_FOUND");
        assertThat(gqlData(bob.token(), "{ friend(id: \"%s\") { id } }".formatted(request)).at("/friend/id").asText())
                .isEqualTo(request.toString());
    }
}
