package com.workouttracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "session_sets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_exercise_id", "set_number"}))
public class SessionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_exercise_id", nullable = false)
    private SessionExercise sessionExercise;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "actual_reps")
    private Integer actualReps;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    // kilograms
    @Column(name = "actual_weight", precision = 7, scale = 2)
    private BigDecimal actualWeight;

    @Column(nullable = false)
    private boolean completed;

    protected SessionSet() { }

    public SessionSet(int setNumber) { this.setNumber = setNumber; }

    public void apply(Integer reps, Integer durationSeconds, BigDecimal weight, boolean completed) {
        this.actualReps = reps;
        this.actualDurationSeconds = durationSeconds;
        this.actualWeight = weight == null ? null : weight.setScale(2, RoundingMode.HALF_UP);
        this.completed = completed;
    }

    public Long getId() { return id; }
    public SessionExercise getSessionExercise() { return sessionExercise; }
    public void setSessionExercise(SessionExercise sessionExercise) { this.sessionExercise = sessionExercise; }
    public int getSetNumber() { return setNumber; }
    public Integer getActualReps() { return actualReps; }
    public Integer getActualDurationSeconds() { return actualDurationSeconds; }
    public BigDecimal getActualWeight() { return actualWeight; }
    public boolean isCompleted() { return completed; }
}