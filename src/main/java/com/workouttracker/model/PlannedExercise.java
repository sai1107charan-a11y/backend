package com.workouttracker.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// One exercise inside a plan, with its targets.
// Exactly one of reps / durationSeconds is set. Weight is stored in kilograms.
@Entity
@Table(name = "planned_exercises")
public class PlannedExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

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

    @Column(name = "target_weight", precision = 7, scale = 2)
    private BigDecimal targetWeight;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    protected PlannedExercise() {
    }

    public PlannedExercise(Exercise exercise, int sortOrder, int sets, Integer reps,
                           Integer durationSeconds, BigDecimal targetWeight, int restSeconds) {
        this.exercise = exercise;
        this.sortOrder = sortOrder;
        this.sets = sets;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.targetWeight = targetWeight;
        this.restSeconds = restSeconds;
    }

    public Long getId() { return id; }
    public WorkoutPlan getPlan() { return plan; }
    public void setPlan(WorkoutPlan plan) { this.plan = plan; }
    public Exercise getExercise() { return exercise; }
    public int getSortOrder() { return sortOrder; }
    public int getSets() { return sets; }
    public Integer getReps() { return reps; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public BigDecimal getTargetWeight() { return targetWeight; }
    public int getRestSeconds() { return restSeconds; }
}