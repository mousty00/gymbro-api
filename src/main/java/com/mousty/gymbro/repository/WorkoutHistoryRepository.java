package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.WorkoutHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkoutHistoryRepository extends JpaRepository<WorkoutHistory, UUID> {
    Page<WorkoutHistory> findAllByUser_Username(String username, Pageable pageable);
    Page<WorkoutHistory> findAllByGroup_Id(UUID groupId, Pageable pageable);
}
