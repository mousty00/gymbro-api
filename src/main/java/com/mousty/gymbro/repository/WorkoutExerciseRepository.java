package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.WorkoutExercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    Page<WorkoutExercise> findAllByWorkout_User_Username(String username, Pageable pageable);
}
