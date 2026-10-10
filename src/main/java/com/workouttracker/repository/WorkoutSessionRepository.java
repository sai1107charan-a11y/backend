package com.workouttracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutSession;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    Optional<WorkoutSession> findByIdAndUser(Long id, User user);

    Optional<WorkoutSession> findFirstByUserAndStatus(User user, WorkoutSession.Status status);

    Optional<WorkoutSession> findByUserAndClientToken(User user, String clientToken);

    List<WorkoutSession> findAllByUserAndStatusOrderBySessionDateDescStartedAtDesc(User user, WorkoutSession.Status status);
}