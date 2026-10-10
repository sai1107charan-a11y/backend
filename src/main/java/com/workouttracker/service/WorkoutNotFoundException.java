package com.workouttracker.service;

// Thrown when we look for a workout id that does not exist in the database.
public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(Long id) {
        super("Workout not found with id: " + id);
    }
}