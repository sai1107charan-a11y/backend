package com.workouttracker.dto;

import com.workouttracker.model.MuscleGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(

        @NotBlank(message = "Exercise name is required")
        @Size(max = 100, message = "Exercise name is too long")
        String name,

        @NotNull(message = "Muscle group is required")
        MuscleGroup muscleGroup,

        @NotBlank(message = "Equipment is required")
        @Size(max = 100, message = "Equipment is too long")
        String equipment,

        @Size(max = 500, message = "Description is too long")
        String description
) {
}