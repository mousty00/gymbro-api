package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    List<Workout> findAllByUser_Username(String userUsername);
}
