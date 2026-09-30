-- Baseline schema, derived from the JPA entities as they exist in production today
-- (this intentionally reflects what ddl-auto=update actually produced, not the
-- drifted sql/gymbro.sql seed script — e.g. it includes verify_otp_expired_at /
-- reset_otp_expired_at, which that script never had).

CREATE TABLE role
(
    id          UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(50)      NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE "user"
(
    id                     UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    username               VARCHAR(50)      NOT NULL UNIQUE,
    email                  VARCHAR(100)     NOT NULL UNIQUE,
    password               VARCHAR(255)     NOT NULL,
    first_name             VARCHAR(50),
    last_name              VARCHAR(50),
    birth_date             DATE             NOT NULL,
    image                  VARCHAR(255),
    role_id                UUID             NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    is_account_verified    BOOLEAN                   DEFAULT FALSE,
    verify_otp             VARCHAR(255),
    verify_otp_expired_at  BIGINT,
    reset_otp              VARCHAR(255),
    reset_otp_expired_at   BIGINT,
    created_at             TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE exercise
(
    id           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name         VARCHAR(100)     NOT NULL,
    description  TEXT,
    muscle_group VARCHAR(50)      NOT NULL,
    is_public    BOOLEAN          NOT NULL DEFAULT TRUE,
    created_by   UUID             REFERENCES "user" (id) ON DELETE SET NULL
);

CREATE TABLE workout
(
    id          UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    name        VARCHAR(100)     NOT NULL,
    description TEXT,
    is_public   BOOLEAN          NOT NULL DEFAULT FALSE,
    day_of_week INT[]                     DEFAULT '{}',
    start_time  TIME,
    created_at  TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE workout_exercise
(
    id           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    workout_id   UUID             NOT NULL REFERENCES workout (id) ON DELETE CASCADE,
    exercise_id  UUID             NOT NULL REFERENCES exercise (id) ON DELETE CASCADE,
    sets         INT              NOT NULL,
    reps         INT,
    weight       DECIMAL(6, 2),
    rest_seconds INT,
    "position"   INT              NOT NULL
);

CREATE TABLE workout_group
(
    id            UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(100)     NOT NULL,
    workout_id    UUID             NOT NULL REFERENCES workout (id) ON DELETE CASCADE,
    created_by    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    scheduled_for TIMESTAMP,
    status        VARCHAR(20)      NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE group_member
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    group_id   UUID             NOT NULL REFERENCES workout_group (id) ON DELETE CASCADE,
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    status     VARCHAR(20)      NOT NULL DEFAULT 'invited',
    created_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    UNIQUE (group_id, user_id)
);

CREATE TABLE workout_history
(
    id           UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    workout_id   UUID             NOT NULL REFERENCES workout (id) ON DELETE CASCADE,
    group_id     UUID             REFERENCES workout_group (id) ON DELETE SET NULL,
    started_at   TIMESTAMP        NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    notes        TEXT
);

CREATE TABLE post
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    content    TEXT             NOT NULL,
    image_url  VARCHAR(255),
    created_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE post_like
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    post_id    UUID             NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    created_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

CREATE TABLE post_comment
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    post_id    UUID             NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    content    TEXT             NOT NULL,
    created_at TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE friendship
(
    id         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    friend_id  UUID             NOT NULL REFERENCES "user" (id) ON DELETE CASCADE,
    status     VARCHAR(20)      NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    CHECK (user_id != friend_id),
    UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_workouts_user_id ON workout (user_id);
CREATE INDEX idx_workout_exercises_workout_id ON workout_exercise (workout_id);
CREATE INDEX idx_workout_history_user_id ON workout_history (user_id);
CREATE INDEX idx_workout_groups_workout_id ON workout_group (workout_id);
CREATE INDEX idx_group_members_group_id ON group_member (group_id);
CREATE INDEX idx_group_members_user_id ON group_member (user_id);
CREATE INDEX idx_post_user_id ON post (user_id);
CREATE INDEX idx_post_like_post_id ON post_like (post_id);
CREATE INDEX idx_post_like_user_id ON post_like (user_id);
CREATE INDEX idx_post_comment_post_id ON post_comment (post_id);

INSERT INTO role (name, description)
VALUES ('Admin', 'Admin account'),
       ('User', 'User account');
