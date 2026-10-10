package com.workouttracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.workouttracker.model.Program;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findAllByOrderByIdAsc();

    boolean existsByName(String name);
}