package com.workouttracker.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// One performed (or in-progress) workout. Plan data is COPIED in, never linked.
@Entity
@Table(name = "workout_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "client_token"}))
public class WorkoutSession {

    public enum Status { IN_PROGRESS, COMPLETED, ABANDONED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Informational only (no foreign key), so deleting a plan never touches history
    @Column(name = "source_plan_id")
    private Long sourcePlanId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "client_token", nullable = false, length = 64)
    private String clientToken;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<SessionExercise> exercises = new ArrayList<>();

    protected WorkoutSession() { }

    public WorkoutSession(User user, Long sourcePlanId, String name, String clientToken, LocalDate sessionDate) {
        this.user = user;
        this.sourcePlanId = sourcePlanId;
        this.name = name;
        this.clientToken = clientToken;
        this.sessionDate = sessionDate;
        this.startedAt = Instant.now();
    }

    public void addExercise(SessionExercise exercise) {
        exercise.setSession(this);
        exercises.add(exercise);
    }

    public void complete(Instant when) {
        this.status = Status.COMPLETED;
        this.finishedAt = when;
    }

    public void abandon(Instant when) {
        this.status = Status.ABANDONED;
        this.finishedAt = when;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Long getSourcePlanId() { return sourcePlanId; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public LocalDate getSessionDate() { return sessionDate; }
    public List<SessionExercise> getExercises() { return exercises; }
}