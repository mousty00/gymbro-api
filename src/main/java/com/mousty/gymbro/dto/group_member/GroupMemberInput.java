package com.mousty.gymbro.dto.group_member;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberInput {

    @NotNull
    private UUID groupId;

    @NotNull
    private String invitedUsername;


}
