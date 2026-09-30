package com.mousty.gymbro.service;

import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutGroupServiceAdapterTest {

    @Mock private WorkoutGroupRepository repository;

    private WorkoutGroupServiceAdapter adapter;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        adapter = new WorkoutGroupServiceAdapter(repository);
    }

    @Test
    @DisplayName("returns the stored group")
    void found() {
        WorkoutGroup group = WorkoutGroup.builder().id(id).build();
        when(repository.findById(id)).thenReturn(Optional.of(group));

        assertThat(adapter.getWorkoutGroupById(id)).isSameAs(group);
    }

    @Test
    @DisplayName("throws IllegalArgumentException for unknown id")
    void notFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.getWorkoutGroupById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workout group not found");
    }
}
