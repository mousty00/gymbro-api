package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.WorkoutGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface WorkoutGroupRepository extends JpaRepository<WorkoutGroup, UUID> {

    String VISIBLE_TO = "from WorkoutGroup g left join g.groupMembers m "
            + "where g.createdBy.username = :username or (m.user.username = :username and m.status = 'accepted')";

    /** Groups the user created or is an accepted member of. */
    @Query(value = "select distinct g " + VISIBLE_TO, countQuery = "select count(distinct g) " + VISIBLE_TO)
    Page<WorkoutGroup> findAllVisibleTo(@Param("username") String username, Pageable pageable);
}
