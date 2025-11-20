package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkoutGroupServiceAdapter {
    private final WorkoutGroupRepository workoutGroupRepository;

    public WorkoutGroup getWorkoutGroupById(UUID id) {
        return workoutGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workout group not found"));
    }
}