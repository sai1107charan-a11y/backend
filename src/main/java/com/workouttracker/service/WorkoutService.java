package com.workouttracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.model.User;
import com.workouttracker.model.Workout;
import com.workouttracker.repository.WorkoutRepository;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;

    public WorkoutService(WorkoutRepository workoutRepository) {
        this.workoutRepository = workoutRepository;
    }

    @Transactional(readOnly = true)
    public List<Workout> getAllWorkouts(User user) {
        return workoutRepository.findAllByUserOrderByWorkoutDateDescIdDesc(user);
    }

    // Someone else's workout looks exactly like a missing one (404),
    // so ids of other users' records are never confirmed.
    @Transactional(readOnly = true)
    public Workout getWorkoutById(User user, Long id) {
        return workoutRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new WorkoutNotFoundException(id));
    }

    @Transactional
    public Workout createWorkout(User user, Workout workout) {
        workout.setId(null);
        workout.setUser(user); // the owner always comes from the session
        return workoutRepository.save(workout);
    }

    @Transactional
    public Workout updateWorkout(User user, Long id, Workout updatedWorkout) {
        Workout existing = getWorkoutById(user, id);

        existing.setExerciseName(updatedWorkout.getExerciseName());
        existing.setSets(updatedWorkout.getSets());
        existing.setReps(updatedWorkout.getReps());
        existing.setWeight(updatedWorkout.getWeight());
        existing.setWorkoutDate(updatedWorkout.getWorkoutDate());

        return workoutRepository.save(existing);
    }

    @Transactional
    public void deleteWorkout(User user, Long id) {
        Workout existing = getWorkoutById(user, id);
        workoutRepository.delete(existing);
    }
}