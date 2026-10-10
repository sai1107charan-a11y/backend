package com.workouttracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.dto.StatsDto;
import com.workouttracker.service.StatsService;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;
    private final CurrentUser currentUser;

    public StatsController(StatsService statsService, CurrentUser currentUser) {
        this.statsService = statsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard")
    public StatsDto.Dashboard dashboard() {
        return statsService.dashboard(currentUser.get());
    }

    @GetMapping("/progress/overview")
    public StatsDto.Overview overview() {
        return statsService.overview(currentUser.get());
    }

    // ?start=2026-10-05 (any day inside the week you want). Default: this week.
    @GetMapping("/progress/week")
    public List<StatsDto.DayStatus> week(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start) {
        return statsService.week(currentUser.get(), start == null ? LocalDate.now() : start);
    }

    @GetMapping("/progress/exercises")
    public List<StatsDto.ExerciseOption> exercises() {
        return statsService.exerciseOptions(currentUser.get());
    }

    @GetMapping("/progress/exercises/{id}")
    public StatsDto.ExerciseProgress exercise(@PathVariable Long id) {
        return statsService.exerciseProgress(currentUser.get(), id);
    }
}