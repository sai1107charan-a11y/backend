package com.workouttracker.dto;

import com.workouttracker.model.UserSettings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class SettingsDto {

    private SettingsDto() { }

    public record Request(
            @NotNull(message = "Weight unit is required") UserSettings.WeightUnit weightUnit,
            @NotNull(message = "Weekly goal is required")
            @Min(value = 1, message = "Weekly goal must be at least 1")
            @Max(value = 14, message = "Weekly goal cannot be more than 14") Integer weeklyGoal,
            @NotNull(message = "Theme is required") UserSettings.Theme theme,
            @NotNull(message = "Default rest time is required")
            @Min(value = 5, message = "Default rest must be at least 5 seconds")
            @Max(value = 900, message = "Default rest cannot be more than 900 seconds") Integer defaultRestSeconds) { }

    public record Response(String weightUnit, int weeklyGoal, String theme, int defaultRestSeconds) { }
}