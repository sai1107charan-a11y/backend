package com.workouttracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "program_days")
public class ProgramDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    // 1 = Monday ... 7 = Sunday (same numbers as java.time.DayOfWeek)
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "rest_day", nullable = false)
    private boolean restDay;

    @OneToMany(mappedBy = "programDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProgramExercise> exercises = new ArrayList<>();

    protected ProgramDay() {
    }

    public ProgramDay(int dayOfWeek, String name, boolean restDay) {
        this.dayOfWeek = dayOfWeek;
        this.name = name;
        this.restDay = restDay;
    }

    public void addExercise(ProgramExercise exercise) {
        exercise.setProgramDay(this);
        exercises.add(exercise);
    }

    public Long getId() { return id; }
    public Program getProgram() { return program; }
    public void setProgram(Program program) { this.program = program; }
    public int getDayOfWeek() { return dayOfWeek; }
    public String getName() { return name; }
    public boolean isRestDay() { return restDay; }
    public List<ProgramExercise> getExercises() { return exercises; }
}