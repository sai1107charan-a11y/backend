package com.workouttracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.Exercise;
import com.workouttracker.model.SessionExercise;

@Repository
public interface SessionExerciseRepository extends JpaRepository<SessionExercise, Long> {

    boolean existsByExercise(Exercise exercise);
}