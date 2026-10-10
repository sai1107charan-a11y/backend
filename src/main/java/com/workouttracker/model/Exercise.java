package com.workouttracker.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// user = null  -> built-in exercise (shared, read-only)
// user != null -> custom exercise that belongs to that user
@Entity
@Table(name = "exercises",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR) // plain VARCHAR, not a MySQL ENUM column
    @Column(name = "muscle_group", nullable = false, length = 30)
    private MuscleGroup muscleGroup;

    @Column(nullable = false, length = 100)
    private String equipment;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    protected Exercise() {
    }

    public Exercise(String name, MuscleGroup muscleGroup, String equipment, String description) {
        this.name = name;
        this.muscleGroup = muscleGroup;
        this.equipment = equipment;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MuscleGroup getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(MuscleGroup muscleGroup) { this.muscleGroup = muscleGroup; }
    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}