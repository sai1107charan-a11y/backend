package com.workouttracker.controller;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.dto.UserDto;
import com.workouttracker.dto.UserResponse;
import com.workouttracker.model.User;
import com.workouttracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public UserResponse me() {
        User user = currentUser.get();
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    @PutMapping("/me")
    public UserResponse rename(@Valid @RequestBody UserDto.UpdateName request) {
        return userService.rename(currentUser.get(), request.name());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody UserDto.ChangePassword request) {
        userService.changePassword(currentUser.get(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}