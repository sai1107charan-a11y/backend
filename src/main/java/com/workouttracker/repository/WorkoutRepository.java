package com.workouttracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.User;
import com.workouttracker.model.Workout;

@Repository
public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findAllByUserOrderByWorkoutDateDescIdDesc(User user);

    // The ownership check: a row only matches if BOTH the id and the owner match.
    Optional<Workout> findByIdAndUser(Long id, User user);
}