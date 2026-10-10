package com.workouttracker.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "session_exercises")
public class SessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    // Name copied at start time (stays correct even if a custom exercise is renamed)
    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "planned_sets", nullable = false)
    private int plannedSets;

    @Column
    private Integer reps;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "target_weight", precision = 7, scale = 2)
    private BigDecimal targetWeight;

    @Column(name = "rest_seconds", nullable = false)
    private int restSeconds;

    @OneToMany(mappedBy = "sessionExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("setNumber ASC")
    private List<SessionSet> setList = new ArrayList<>();

    protected SessionExercise() { }

    public SessionExercise(Exercise exercise, int sortOrder, int plannedSets, Integer reps,
                           Integer durationSeconds, BigDecimal targetWeight, int restSeconds) {
        this.exercise = exercise;
        this.exerciseName = exercise.getName();
        this.sortOrder = sortOrder;
        this.plannedSets = plannedSets;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.targetWeight = targetWeight;
        this.restSeconds = restSeconds;
    }

    public void addSet(SessionSet set) {
        set.setSessionExercise(this);
        setList.add(set);
    }

    public Long getId() { return id; }
    public WorkoutSession getSession() { return session; }
    public void setSession(WorkoutSession session) { this.session = session; }
    public Exercise getExercise() { return exercise; }
    public String getExerciseName() { return exerciseName; }
    public int getSortOrder() { return sortOrder; }
    public int getPlannedSets() { return plannedSets; }
    public Integer getReps() { return reps; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public BigDecimal getTargetWeight() { return targetWeight; }
    public int getRestSeconds() { return restSeconds; }
    public List<SessionSet> getSetList() { return setList; }
}