package com.workouttracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDto {

    private UserDto() { }

    public record UpdateName(@NotBlank(message = "Name is required")
                             @Size(max = 100, message = "Name is too long") String name) { }

    public record ChangePassword(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 72, message = "New password must be 6 to 72 characters") String newPassword) { }
}