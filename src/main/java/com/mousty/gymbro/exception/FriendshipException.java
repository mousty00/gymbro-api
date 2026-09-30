package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class FriendshipException extends GymBroException {

    private FriendshipException(String message, HttpStatus status) {
        super(message, status);
    }

    public static FriendshipException notFound(UUID id) {
        return new FriendshipException("Friend request not found: " + id, HttpStatus.NOT_FOUND);
    }

    public static FriendshipException notFound() {
        return new FriendshipException("Friend request not found", HttpStatus.NOT_FOUND);
    }

    public static FriendshipException unauthorized() {
        return new FriendshipException("Not authorized to perform this action on this friendship", HttpStatus.FORBIDDEN);
    }

    public static FriendshipException cannotBefriendSelf() {
        return new FriendshipException("You cannot send a friend request to yourself", HttpStatus.BAD_REQUEST);
    }

    public static FriendshipException alreadyFriends() {
        return new FriendshipException("Users are already friends or a request is pending", HttpStatus.CONFLICT);
    }
}
