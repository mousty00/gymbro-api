package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.Exercise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    Page<Exercise> findAllByIsPublicTrueOrCreatedBy_Username(String username, Pageable pageable);
}
