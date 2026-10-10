package com.workouttracker.model;

public enum ProgramCategory {
    BODYBUILDING("Bodybuilding"),
    WEIGHT_LOSS("Weight Loss"),
    CALISTHENICS("Calisthenics"),
    ABS_CORE("Abs and Core"),
    CARDIO("Cardio Training"),
    FLEXIBILITY("Flexibility and Mobility");

    private final String label;

    ProgramCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}