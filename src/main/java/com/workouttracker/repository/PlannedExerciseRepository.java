package com.workouttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.Exercise;
import com.workouttracker.model.PlannedExercise;

@Repository
public interface PlannedExerciseRepository extends JpaRepository<PlannedExercise, Long> {

    boolean existsByExercise(Exercise exercise);
}