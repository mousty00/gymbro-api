package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByUsername(@Size(max = 50) @NotNull String username);

    boolean existsUsersByUsername(@Size(max = 50) @NotNull String username);

    boolean existsUsersByEmail(@Size(max = 100) @NotNull String email);

    Optional<User> findByEmail(@Size(max = 100) @NotNull String email);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:username%")
    List<User> findAllByUsernameLike(String username);

    @Size(max = 255)
    @NotNull String getUserById(UUID id);
}
