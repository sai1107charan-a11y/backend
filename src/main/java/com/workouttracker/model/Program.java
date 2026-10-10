package com.workouttracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

// A built-in, read-only program template. Users get their own COPY in Batch 3.
@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private ProgramCategory category;

    @Column(nullable = false, length = 1000)
    private String description;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC")
    private List<ProgramDay> days = new ArrayList<>();

    protected Program() {
    }

    public Program(String name, ProgramCategory category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public void addDay(ProgramDay day) {
        day.setProgram(this);
        days.add(day);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProgramCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public List<ProgramDay> getDays() { return days; }
}