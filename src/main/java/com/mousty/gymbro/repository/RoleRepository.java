package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findRoleByName(@Size(max = 50) @NotNull String name);

    Role getRolesByName(String name);
}
