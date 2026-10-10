package com.workouttracker.dto;

// JSON sent back after register/login. It has NO password field.
public record UserResponse(Long id, String name, String email) {
}