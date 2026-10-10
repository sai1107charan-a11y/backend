package com.workouttracker.dto;

import com.workouttracker.model.Exercise;

public record ExerciseResponse(
        Long id,
        String name,
        String muscleGroup,
        String muscleGroupLabel,
        String equipment,
        String description,
        boolean custom
) {
    public static ExerciseResponse from(Exercise e) {
        return new ExerciseResponse(
                e.getId(),
                e.getName(),
                e.getMuscleGroup().name(),
                e.getMuscleGroup().getLabel(),
                e.getEquipment(),
                e.getDescription(),
                e.getUser() != null);
    }
}