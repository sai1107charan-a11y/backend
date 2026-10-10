package com.workouttracker.dto;

import java.math.BigDecimal;
import java.util.List;

import com.workouttracker.model.MuscleGroup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PlanDto {

    private PlanDto() {
    }

    // ---------- Requests ----------

    public record Request(
            @Size(max = 100, message = "Workout name is too long (max 100)")
            String name,

            MuscleGroup category,

            @Min(value = 1, message = "Day must be between Monday and Sunday")
            @Max(value = 7, message = "Day must be between Monday and Sunday")
            Integer dayOfWeek,

            boolean restDay,

            @Valid
            @Size(max = 30, message = "A workout can have at most 30 exercises")
            List<Item> exercises
    ) {
    }

    // Send reps OR durationSeconds (not both)
    public record Item(
            @NotNull(message = "Choose an exercise")
            Long exerciseId,

            @NotNull(message = "Sets is required")
            @Min(value = 1, message = "Sets must be at least 1")
            @Max(value = 20, message = "Sets cannot be more than 20")
            Integer sets,

            @Min(value = 1, message = "Reps must be at least 1")
            @Max(value = 1000, message = "Reps cannot be more than 1000")
            Integer reps,

            @Min(value = 1, message = "Duration must be at least 1 second")
            @Max(value = 36000, message = "Duration is too long")
            Integer durationSeconds,

            // kilograms
            @DecimalMin(value = "0.0", message = "Weight cannot be negative")
            @DecimalMax(value = "1000.0", message = "Weight is too large")
            BigDecimal targetWeight,

            @NotNull(message = "Rest time is required")
            @Min(value = 0, message = "Rest cannot be negative")
            @Max(value = 900, message = "Rest cannot be more than 900 seconds")
            Integer restSeconds
    ) {
    }

    // ---------- Responses ----------

    public record Response(Long id, String name, String category, String categoryLabel,
                           Integer dayOfWeek, String dayName, boolean restDay,
                           String sourceProgram, List<ItemResponse> exercises) {
    }

    public record ItemResponse(Long exerciseId, String exerciseName, String muscleGroupLabel,
                               int sets, Integer reps, Integer durationSeconds,
                               BigDecimal targetWeight, int restSeconds) {
    }
}