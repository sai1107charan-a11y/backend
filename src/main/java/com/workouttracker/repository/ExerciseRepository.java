package com.workouttracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.Exercise;
import com.workouttracker.model.User;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    // Everything this user may see: built-in exercises + their own custom ones
    @Query("select e from Exercise e where e.user is null or e.user = :user order by e.name")
    List<Exercise> findVisibleTo(@Param("user") User user);

    // Same rule for a single id. Another user's custom exercise is NOT found.
    @Query("select e from Exercise e where e.id = :id and (e.user is null or e.user = :user)")
    Optional<Exercise> findVisibleById(@Param("id") Long id, @Param("user") User user);

    // Used by the seeder
    List<Exercise> findAllByUserIsNull();

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);

    Optional<Exercise> findByNameIgnoreCaseAndUserIsNull(String name);
}