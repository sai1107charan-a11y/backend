package com.workouttracker.dto;

import java.util.List;

// Response shapes for the program endpoints.
public final class ProgramDto {

    private ProgramDto() {
    }

    public record Summary(Long id, String name, String category, String categoryLabel,
                          String description, int trainingDays, int restDays, int exerciseCount) {
    }

    public record Detail(Long id, String name, String category, String categoryLabel,
                         String description, int trainingDays, int restDays, List<Day> days) {
    }

    public record Day(int dayOfWeek, String dayName, String name, boolean restDay, List<Item> exercises) {
    }

    // reps is used for strength work, durationSeconds for timed work (one of them is null)
    public record Item(Long exerciseId, String exerciseName, String muscleGroupLabel, String equipment,
                       int sets, Integer reps, Integer durationSeconds, int restSeconds) {
    }
}