package com.workouttracker.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.dto.ProgramDto;
import com.workouttracker.service.ProgramService;

// Templates are shared and read-only, so there is no ownership check here.
// The /api/** rule in SecurityConfig still requires a logged-in user.
@RestController
@RequestMapping("/api/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    public List<ProgramDto.Summary> list() {
        return programService.list();
    }

    @GetMapping("/{id}")
    public ProgramDto.Detail get(@PathVariable Long id) {
        return programService.get(id);
    }
}