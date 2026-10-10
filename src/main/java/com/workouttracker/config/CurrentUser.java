package com.workouttracker.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.workouttracker.model.User;
import com.workouttracker.repository.UserRepository;
import com.workouttracker.service.InvalidCredentialsException;

// Returns the User that owns the current session (from the security context).
@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InvalidCredentialsException();
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(InvalidCredentialsException::new);
    }
}