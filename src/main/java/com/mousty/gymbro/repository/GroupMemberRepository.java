package com.mousty.gymbro.repository;

import com.mousty.gymbro.entity.GroupMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    boolean existsByUserIdAndGroup_Id(UUID userId, UUID groupId);

    void deleteGroupMemberById(UUID id);

    Page<GroupMember> findAllByGroup_Id(UUID groupId, Pageable pageable);
}
