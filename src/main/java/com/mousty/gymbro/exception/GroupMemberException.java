package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class GroupMemberException extends GymBroException {

    private GroupMemberException(String message, HttpStatus status) {
        super(message, status);
    }

    public static GroupMemberException notFound(UUID id) {
        return new GroupMemberException("Group member not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static GroupMemberException notFound() {
        return new GroupMemberException("Group member not found", HttpStatus.NOT_FOUND);
    }

    public static GroupMemberException alreadyMember() {
        return new GroupMemberException("User is already a member of this group", HttpStatus.CONFLICT);
    }

    public static GroupMemberException unauthorized() {
        return new GroupMemberException("Not authorized to perform this action on this group member", HttpStatus.FORBIDDEN);
    }
}
