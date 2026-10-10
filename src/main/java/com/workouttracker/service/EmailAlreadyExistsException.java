package com.workouttracker.service;

// Thrown when someone registers with an email that is already used.
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException() {
        super("Email already registered");
    }
}