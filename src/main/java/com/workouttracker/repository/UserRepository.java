package com.workouttracker.repository;

import com.workouttracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Used during registration to reject an email that is already taken
    boolean existsByEmail(String email);
}