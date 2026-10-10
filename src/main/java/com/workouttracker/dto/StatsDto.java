package com.workouttracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class StatsDto {

    private StatsDto() { }

    public record Overview(long totalCompleted, int completedThisWeek, int weeklyGoal, int remaining,
                           int completionPercent, int streakWeeks, List<Frequent> mostFrequent) { }

    public record Frequent(Long exerciseId, String name, int sessions, int sets) { }

    public record ExerciseOption(Long id, String name) { }

    public record Best(BigDecimal weightKg, Integer reps, LocalDate date) { }

    public record Point(LocalDate date, BigDecimal weightKg, Integer reps) { }

    public record ExerciseProgress(Long exerciseId, String name, Best personalBest, Best latest,
                                   Best previous, List<Point> history) { }

    public record Done(Long id, String name) { }

    public record DayStatus(int dayOfWeek, String dayName, LocalDate date, boolean today,
                            String plannedName, boolean restDay, List<Done> completed) { }

    public record TodayPlan(Long planId, String name, boolean restDay, int exerciseCount) { }

    public record ActiveInfo(Long id, String name) { }

    public record Strength(Long exerciseId, String name, BigDecimal previousBest, LocalDate previousDate,
                           BigDecimal latestBest, LocalDate latestDate) { }

    public record Dashboard(long totalCompleted, int completedThisWeek, int weeklyGoal, int remaining,
                            int completionPercent, int streakWeeks, String currentProgram,
                            TodayPlan today, ActiveInfo active, List<SessionDto.Summary> recent,
                            List<DayStatus> week, List<Strength> strength) { }
}