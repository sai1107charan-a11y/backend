package com.workouttracker.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.ExerciseRequest;
import com.workouttracker.dto.ExerciseResponse;
import com.workouttracker.exception.ApiException;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.model.User;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.PlannedExerciseRepository;

import com.workouttracker.repository.SessionExerciseRepository;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final PlannedExerciseRepository plannedExerciseRepository;
    private final SessionExerciseRepository sessionExerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository,
                           PlannedExerciseRepository plannedExerciseRepository,
                           SessionExerciseRepository sessionExerciseRepository) {
        this.exerciseRepository = exerciseRepository;
        this.plannedExerciseRepository = plannedExerciseRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
    }

    // The library is small (tens to a few hundred rows), so we filter in Java.
    // This keeps the code simple and avoids tricky null-parameter queries.
    @Transactional(readOnly = true)
    public List<ExerciseResponse> search(User user, String search, MuscleGroup group) {
        String term = search == null ? "" : search.trim().toLowerCase();

        return exerciseRepository.findVisibleTo(user).stream()
                .filter(e -> group == null || e.getMuscleGroup() == group)
                .filter(e -> term.isEmpty()
                        || e.getName().toLowerCase().contains(term)
                        || e.getEquipment().toLowerCase().contains(term))
                .map(ExerciseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseResponse get(User user, Long id) {
        return ExerciseResponse.from(findVisible(user, id));
    }

    @Transactional
    public ExerciseResponse create(User user, ExerciseRequest request) {
        String name = request.name().trim();
        ensureNameIsFree(user, name, null);

        Exercise exercise = new Exercise(name, request.muscleGroup(),
                request.equipment().trim(), cleanDescription(request.description()));
        exercise.setUser(user); // the owner always comes from the session
        return ExerciseResponse.from(exerciseRepository.save(exercise));
    }

    @Transactional
    public ExerciseResponse update(User user, Long id, ExerciseRequest request) {
        Exercise exercise = findOwnCustom(user, id);
        String name = request.name().trim();
        ensureNameIsFree(user, name, id);

        exercise.setName(name);
        exercise.setMuscleGroup(request.muscleGroup());
        exercise.setEquipment(request.equipment().trim());
        exercise.setDescription(cleanDescription(request.description()));
        return ExerciseResponse.from(exercise); // saved automatically when the transaction ends
    }

    @Transactional
    public void delete(User user, Long id) {
        Exercise exercise = findOwnCustom(user, id);
        if (plannedExerciseRepository.existsByExercise(exercise)
                || sessionExerciseRepository.existsByExercise(exercise)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This exercise is used in your workouts or workout history, so it cannot be deleted.");
        }
        exerciseRepository.delete(exercise);
    }

    // ---------- helpers ----------

    private Exercise findVisible(User user, Long id) {
        return exerciseRepository.findVisibleById(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise not found"));
    }

    // Built-in -> 403. Another user's custom exercise -> 404 (looks like it does not exist).
    private Exercise findOwnCustom(User user, Long id) {
        Exercise exercise = findVisible(user, id);
        if (exercise.getUser() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Built-in exercises cannot be changed");
        }
        return exercise;
    }

    private void ensureNameIsFree(User user, String name, Long ignoreId) {
        boolean taken = exerciseRepository.findVisibleTo(user).stream()
                .anyMatch(e -> e.getName().equalsIgnoreCase(name) && !e.getId().equals(ignoreId));
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "An exercise with this name already exists");
        }
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}