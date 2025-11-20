package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.WorkoutGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkoutGroupRepository extends JpaRepository<WorkoutGroup, UUID> {
}
