package com.workouttracker.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.RegisterRequest;
import com.workouttracker.dto.UserResponse;
import com.workouttracker.model.User;
import com.workouttracker.repository.UserRepository;
import com.workouttracker.exception.ApiException;
import org.springframework.http.HttpStatus;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        return toResponse(user);
    }

    @Transactional
    public UserResponse rename(User current, String name) {
        User user = userRepository.findById(current.getId()).orElseThrow(InvalidCredentialsException::new);
        user.setName(name.trim());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User current, String currentPassword, String newPassword) {
        User user = userRepository.findById(current.getId()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}