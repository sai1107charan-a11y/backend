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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.dto.ExerciseRequest;
import com.workouttracker.dto.ExerciseResponse;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.service.ExerciseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final CurrentUser currentUser;

    public ExerciseController(ExerciseService exerciseService, CurrentUser currentUser) {
        this.exerciseService = exerciseService;
        this.currentUser = currentUser;
    }

    // GET /api/exercises?search=bench&group=CHEST
    @GetMapping
    public List<ExerciseResponse> list(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) MuscleGroup group) {
        return exerciseService.search(currentUser.get(), search, group);
    }

    @GetMapping("/{id}")
    public ExerciseResponse get(@PathVariable Long id) {
        return exerciseService.get(currentUser.get(), id);
    }

    @PostMapping
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody ExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseService.create(currentUser.get(), request));
    }

    @PutMapping("/{id}")
    public ExerciseResponse update(@PathVariable Long id, @Valid @RequestBody ExerciseRequest request) {
        return exerciseService.update(currentUser.get(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        exerciseService.delete(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }
}