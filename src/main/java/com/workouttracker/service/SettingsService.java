package com.workouttracker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.SettingsDto;
import com.workouttracker.model.User;
import com.workouttracker.model.UserSettings;
import com.workouttracker.repository.UserSettingsRepository;

@Service
public class SettingsService {

    private static final SettingsDto.Response DEFAULTS = new SettingsDto.Response("KG", 3, "LIGHT", 60);

    private final UserSettingsRepository repository;

    public SettingsService(UserSettingsRepository repository) {
        this.repository = repository;
    }

    // No row yet? Return the defaults (nothing is inserted on read).
    public SettingsDto.Response view(User user) {
        return repository.findByUser(user).map(this::toResponse).orElse(DEFAULTS);
    }

    @Transactional
    public SettingsDto.Response update(User user, SettingsDto.Request request) {
        UserSettings settings = repository.findByUser(user).orElseGet(() -> new UserSettings(user));
        settings.setWeightUnit(request.weightUnit());
        settings.setWeeklyGoal(request.weeklyGoal());
        settings.setTheme(request.theme());
        settings.setDefaultRestSeconds(request.defaultRestSeconds());
        return toResponse(repository.save(settings));
    }

    private SettingsDto.Response toResponse(UserSettings s) {
        return new SettingsDto.Response(s.getWeightUnit().name(), s.getWeeklyGoal(),
                s.getTheme().name(), s.getDefaultRestSeconds());
    }
}