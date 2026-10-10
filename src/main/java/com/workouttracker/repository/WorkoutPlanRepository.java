package com.workouttracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    List<WorkoutPlan> findAllByUser(User user);

    // Ownership check: matches only if the id AND the owner match
    Optional<WorkoutPlan> findByIdAndUser(Long id, User user);

    Optional<WorkoutPlan> findByUserAndDayOfWeek(User user, Integer dayOfWeek);

    List<WorkoutPlan> findAllByUserAndDayOfWeekIsNotNull(User user);
}