package com.workouttracker.service;

// Thrown when the email or password is wrong at login.
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}