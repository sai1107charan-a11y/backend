package com.workouttracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

// A workout routine that belongs to one user.
// dayOfWeek = 1..7 -> it is scheduled on that weekday. null -> a saved routine, not on the week.
// restDay = true   -> a rest day (needs a dayOfWeek, has no exercises).
@Entity
@Table(name = "workout_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "day_of_week"}))
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private MuscleGroup category;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "rest_day", nullable = false)
    private boolean restDay;

    // The template this plan was copied from (null for custom routines)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_program_id")
    private Program sourceProgram;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PlannedExercise> exercises = new ArrayList<>();

    protected WorkoutPlan() {
    }

    public WorkoutPlan(User user, String name, MuscleGroup category, Integer dayOfWeek, boolean restDay) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.dayOfWeek = dayOfWeek;
        this.restDay = restDay;
    }

    public void addExercise(PlannedExercise exercise) {
        exercise.setPlan(this);
        exercises.add(exercise);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MuscleGroup getCategory() { return category; }
    public void setCategory(MuscleGroup category) { this.category = category; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public boolean isRestDay() { return restDay; }
    public void setRestDay(boolean restDay) { this.restDay = restDay; }
    public Program getSourceProgram() { return sourceProgram; }
    public void setSourceProgram(Program sourceProgram) { this.sourceProgram = sourceProgram; }
    public List<PlannedExercise> getExercises() { return exercises; }
}