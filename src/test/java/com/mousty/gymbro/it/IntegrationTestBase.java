package com.mousty.gymbro.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mousty.gymbro.email.EmailService;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for integration tests: one Postgres container and one Spring context for all
 * IT classes (keep the config identical in subclasses so the context stays cached).
 * The DB is shared and never cleaned, so every test creates its own uniquely named users.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    // ponytail: singleton container started once per JVM, not @Container: the JUnit extension
    // would stop it after each test class while the cached Spring context still points at it.
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    protected static final String PASSWORD = "password123";
    protected static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @MockitoBean
    protected EmailService emailService;

    protected record TestUser(UUID id, String username, String email, String token, String refreshToken) {
    }

    // ---------- users ----------

    protected static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String emailOf(String username) {
        return username + "@example.com";
    }

    /** REST signup only (unverified account). Returns the username. */
    protected String signup(String prefix) throws Exception {
        final String username = unique(prefix);
        rest(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(Map.of(
                "username", username,
                "email", emailOf(username),
                "password", PASSWORD,
                "firstName", "First",
                "lastName", "Last",
                "birthDate", "1990-01-01"))), null)
                .andExpect(status().isCreated());
        return username;
    }

    /** Signup, verify the emailed OTP, log in. */
    protected TestUser signupAndVerify(String prefix) throws Exception {
        final String username = signup(prefix);
        postJson("/auth/verify-otp", Map.of("username", username, "otp", lastVerifyOtp(emailOf(username))), null)
                .andExpect(status().isOk());
        final JsonNode login = body(postJson("/auth/login", Map.of("username", username, "password", PASSWORD), null)
                .andExpect(status().isOk()));
        return new TestUser(userId(username), username, emailOf(username),
                login.get("token").asText(), login.get("refreshToken").asText());
    }

    protected String lastVerifyOtp(String email) {
        final ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendOtpEmail(eq(email), any(), otp.capture());
        return otp.getValue();
    }

    protected String lastResetOtp(String email) {
        final ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendResetOtpEmail(eq(email), any(), otp.capture());
        return otp.getValue();
    }

    protected UUID userId(String username) {
        return jdbc.queryForObject("select id from \"user\" where username = ?", UUID.class, username);
    }

    /** Posts need an S3 upload through the API, so tests insert them directly. */
    protected UUID insertPost(UUID userId, String content) {
        return jdbc.queryForObject("insert into post(user_id, content) values (?, ?) returning id",
                UUID.class, userId, content);
    }

    protected UUID createWorkout(TestUser owner, boolean isPublic) throws Exception {
        final JsonNode data = gqlData(owner.token(), """
                mutation { createWorkout(request: {name: "Leg day", isPublic: %s, dayOfWeek: [1, 3]}) { result { id } } }
                """.formatted(isPublic));
        return UUID.fromString(data.at("/createWorkout/result/id").asText());
    }

    // ---------- HTTP ----------

    /** Every request gets a random client IP, so the per-IP /auth/** rate limit never trips across tests. */
    protected ResultActions rest(MockHttpServletRequestBuilder request, String token) throws Exception {
        request.with(r -> {
            r.setRemoteAddr(randomIp());
            return r;
        });
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    protected ResultActions postJson(String url, Object body, String token) throws Exception {
        return rest(post(url).contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)), token);
    }

    protected static JsonNode body(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString());
    }

    /** POST /graphql; returns the whole response ({data, errors}). */
    protected JsonNode gql(String token, String query) throws Exception {
        MvcResult result = rest(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("query", query))), token).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result = mvc.perform(asyncDispatch(result)).andReturn();
        }
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    /** GraphQL call expected to succeed: asserts no errors and returns data. */
    protected JsonNode gqlData(String token, String query) throws Exception {
        final JsonNode response = gql(token, query);
        assertThat(response.path("errors").isMissingNode() || response.path("errors").isEmpty())
                .as("unexpected GraphQL errors: %s", response.path("errors"))
                .isTrue();
        return response.get("data");
    }

    protected static String errorType(JsonNode response) {
        return response.at("/errors/0/extensions/errorType").asText();
    }

    protected static String errorMessage(JsonNode response) {
        return response.at("/errors/0/message").asText();
    }

    /** The given field of each element of a JSON array, e.g. the ids of a results list. */
    protected static List<String> each(JsonNode array, String field) {
        final List<String> values = new java.util.ArrayList<>();
        array.forEach(node -> values.add(node.path(field).asText()));
        return values;
    }

    private static String randomIp() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
    }
}
