package com.workouttracker.model;

public enum MuscleGroup {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    LEGS("Legs"),
    CORE("Abs and Core"),
    CARDIO("Cardio"),
    FULL_BODY("Full Body"),
    CALISTHENICS("Calisthenics"),
    FLEXIBILITY("Flexibility and Mobility");

    private final String label;

    MuscleGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}