package com.workouttracker.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SessionDto {

    private SessionDto() { }

    public record StartRequest(
            @NotNull(message = "Choose a workout to start") Long planId,
            @NotBlank(message = "Missing request token")
            @Size(max = 64, message = "Invalid request token") String clientToken) { }

    // Actual values for one set. Weight is in kilograms.
    public record SetUpdate(
            @Min(value = 1, message = "Reps must be at least 1")
            @Max(value = 1000, message = "Reps cannot be more than 1000") Integer actualReps,
            @Min(value = 1, message = "Seconds must be at least 1")
            @Max(value = 36000, message = "Seconds is too large") Integer actualDurationSeconds,
            @DecimalMin(value = "0.0", message = "Weight cannot be negative")
            @DecimalMax(value = "1000.0", message = "Weight is too large") BigDecimal actualWeight,
            boolean completed) { }

    public record SetResponse(Long id, int setNumber, Integer actualReps, Integer actualDurationSeconds,
                              BigDecimal actualWeight, boolean completed) { }

    public record ExerciseResponse(Long id, Long exerciseId, String exerciseName, String description,
                                   int plannedSets, Integer reps, Integer durationSeconds,
                                   BigDecimal targetWeight, int restSeconds, List<SetResponse> sets) { }

    public record Response(Long id, Long sourcePlanId, String name, String status, LocalDate date,
                           Instant startedAt, Instant finishedAt, Long durationMinutes,
                           int completedSets, int totalSets, List<ExerciseResponse> exercises) { }

    public record Summary(Long id, String name, LocalDate date, Instant startedAt, long durationMinutes,
                          int completedSets, int totalSets, List<String> exerciseNames) { }

    public record DateCount(LocalDate date, int count) { }
}