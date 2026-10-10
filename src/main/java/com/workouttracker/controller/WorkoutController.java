package com.workouttracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.model.Workout;
import com.workouttracker.service.WorkoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final CurrentUser currentUser;

    public WorkoutController(WorkoutService workoutService, CurrentUser currentUser) {
        this.workoutService = workoutService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Workout> getAllWorkouts() {
        return workoutService.getAllWorkouts(currentUser.get());
    }

    @GetMapping("/{id}")
    public Workout getWorkoutById(@PathVariable Long id) {
        return workoutService.getWorkoutById(currentUser.get(), id);
    }

    @PostMapping
    public ResponseEntity<Workout> createWorkout(@Valid @RequestBody Workout workout) {
        Workout saved = workoutService.createWorkout(currentUser.get(), workout);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Workout updateWorkout(@PathVariable Long id, @Valid @RequestBody Workout workout) {
        return workoutService.updateWorkout(currentUser.get(), id, workout);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkout(@PathVariable Long id) {
        workoutService.deleteWorkout(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }
}