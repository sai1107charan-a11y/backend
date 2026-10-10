package com.workouttracker.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "workouts")
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Exercise name is required")
    @Column(name = "exercise_name", nullable = false)
    private String exerciseName;

    @NotNull(message = "Sets is required")
    @Positive(message = "Sets must be a positive number")
    @Column(nullable = false)
    private Integer sets;

    @NotNull(message = "Reps is required")
    @Positive(message = "Reps must be a positive number")
    @Column(nullable = false)
    private Integer reps;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight cannot be negative")
    @Column(nullable = false)
    private Double weight;

    @NotNull(message = "Workout date is required")
    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    // Owner. Nullable ONLY so rows created before accounts existed keep working
    // until you assign them (see migration steps). Never sent to or read from JSON.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Workout() {
    }

    public Workout(String exerciseName, Integer sets, Integer reps, Double weight, LocalDate workoutDate) {
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.workoutDate = workoutDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public LocalDate getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDate workoutDate) { this.workoutDate = workoutDate; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}