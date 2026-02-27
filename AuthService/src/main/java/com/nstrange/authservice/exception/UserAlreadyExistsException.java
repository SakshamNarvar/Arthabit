package com.nstrange.authservice.exception;

public class UserAlreadyExistsException extends RuntimeException {

    private final String username;

    public UserAlreadyExistsException(String username) {
        super("User with username '" + username + "' already exists");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

