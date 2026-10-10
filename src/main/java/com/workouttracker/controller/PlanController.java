package com.workouttracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workouttracker.config.CurrentUser;
import com.workouttracker.dto.PlanDto;
import com.workouttracker.service.PlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;
    private final CurrentUser currentUser;

    public PlanController(PlanService planService, CurrentUser currentUser) {
        this.planService = planService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<PlanDto.Response> list() {
        return planService.list(currentUser.get());
    }

    @GetMapping("/{id}")
    public PlanDto.Response get(@PathVariable Long id) {
        return planService.get(currentUser.get(), id);
    }

    @PostMapping
    public ResponseEntity<PlanDto.Response> create(@Valid @RequestBody PlanDto.Request request,
                                                   @RequestParam(defaultValue = "false") boolean replace) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.create(currentUser.get(), request, replace));
    }

    @PutMapping("/{id}")
    public PlanDto.Response update(@PathVariable Long id, @Valid @RequestBody PlanDto.Request request,
                                   @RequestParam(defaultValue = "false") boolean replace) {
        return planService.update(currentUser.get(), id, request, replace);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planService.delete(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/plans/5/duplicate -> new saved routine "(copy)" that is not on the week
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<PlanDto.Response> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.duplicate(currentUser.get(), id));
    }

    // POST /api/plans/5/copy?day=3&replace=false -> copy to Wednesday
    @PostMapping("/{id}/copy")
    public ResponseEntity<PlanDto.Response> copy(@PathVariable Long id, @RequestParam int day,
                                                 @RequestParam(defaultValue = "false") boolean replace) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.copyToDay(currentUser.get(), id, day, replace));
    }

    // PUT /api/plans/5/day?day=3 -> schedule on Wednesday. Without "day": take it off the week.
    @PutMapping("/{id}/day")
    public PlanDto.Response moveToDay(@PathVariable Long id, @RequestParam(required = false) Integer day,
                                      @RequestParam(defaultValue = "false") boolean replace) {
        return planService.moveToDay(currentUser.get(), id, day, replace);
    }

    // POST /api/plans/from-program/2?replace=false  ("Use This Plan")
    @PostMapping("/from-program/{programId}")
    public ResponseEntity<List<PlanDto.Response>> fromProgram(@PathVariable Long programId,
                                                              @RequestParam(defaultValue = "false") boolean replace) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.fromProgram(currentUser.get(), programId, replace));
    }

    @PostMapping("/sample-schedule")
    public ResponseEntity<List<PlanDto.Response>> sampleSchedule(
            @RequestParam(defaultValue = "false") boolean replace) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.sampleSchedule(currentUser.get(), replace));
    }
}