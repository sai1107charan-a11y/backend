package com.workouttracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One exercise inside one day of a program, with its targets.
// Strength: sets + reps. Timed (cardio, planks, stretches): sets + durationSeconds.
@Entity
@Table(name = "program_exercises")
public class ProgramExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_day_id", nullable = false)
    private ProgramDay programDay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private int sets;

    @Column
    private Integer reps;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    protected ProgramExercise() {
    }

    public ProgramExercise(Exercise exercise, int sortOrder, int sets, Integer reps,
                           Integer durationSeconds, int restSeconds) {
        this.exercise = exercise;
        this.sortOrder = sortOrder;
        this.sets = sets;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.restSeconds = restSeconds;
    }

    public Long getId() { return id; }
    public ProgramDay getProgramDay() { return programDay; }
    public void setProgramDay(ProgramDay programDay) { this.programDay = programDay; }
    public Exercise getExercise() { return exercise; }
    public int getSortOrder() { return sortOrder; }
    public int getSets() { return sets; }
    public Integer getReps() { return reps; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public int getRestSeconds() { return restSeconds; }
}