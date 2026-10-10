package com.workouttracker.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.dto.SettingsDto;
import com.workouttracker.service.SettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CurrentUser currentUser;

    public SettingsController(SettingsService settingsService, CurrentUser currentUser) {
        this.settingsService = settingsService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public SettingsDto.Response get() {
        return settingsService.view(currentUser.get());
    }

    @PutMapping
    public SettingsDto.Response update(@Valid @RequestBody SettingsDto.Request request) {
        return settingsService.update(currentUser.get(), request);
    }
}