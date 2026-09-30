package com.mousty.gymbro.mapper;

import com.mousty.gymbro.entity.GroupMember;
import com.mousty.gymbro.generic.GenericMapper;
import com.mousty.gymbro.dto.group_member.GroupMemberDTO;
import com.mousty.gymbro.dto.group_member.GroupMemberInput;
import com.mousty.gymbro.service.UserService;
import com.mousty.gymbro.entity.WorkoutGroup;
import com.mousty.gymbro.repository.WorkoutGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GroupMemberMapper implements GenericMapper<GroupMember, GroupMemberDTO> {
    private final UserMapper userMapper;
    private final UserService userService;
    private final WorkoutGroupRepository workoutGroupRepository;

    @Override
    public GroupMemberDTO toDTO(final GroupMember groupMember) {
        return GroupMemberDTO.builder()
                .id(groupMember.getId())
                .workoutGroupId(groupMember.getGroup().getId())
                .user(userMapper.toSimpleDTO(groupMember.getUser(), userService.generateImageUrl(groupMember.getUser())))
                .createdAt(groupMember.getCreatedAt())
                .status(groupMember.getStatus())
                .build();
    }

    @Override
    public GroupMember toEntity(final GroupMemberDTO dto) {
        return GroupMember.builder()
                .id(dto.id())
                .status(dto.status())
                .createdAt(dto.createdAt())
                .user(userService.getUserEntityById(dto.user().id()))
                .group(workoutGroupRepository.findById(dto.workoutGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Workout group not found")))
                .build();
    }


    public GroupMember toNewEntity(final GroupMemberInput dto) {
        return GroupMember.builder()
                .status("invited")
                .createdAt(Instant.now())
                .user(userService.getUserEntityByUsername(dto.invitedUsername()))
                .group(WorkoutGroup.builder()
                        .id(dto.groupId())
                        .build())
                .build();
    }

    public List<GroupMember> toEntityList(final List<GroupMemberDTO> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .toList();
    }

    public List<GroupMemberDTO> toDTOList(final List<GroupMember> members) {
        return members.stream()
                .map(this::toDTO)
                .toList();
    }

}
