package com.workouttracker.dto;

import jakarta.validation.constraints.NotBlank;

// JSON received by POST /api/auth/login
public record LoginRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}