package com.workouttracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import com.workouttracker.dto.SessionDto;
import com.workouttracker.service.SessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workout-sessions")
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUser currentUser;

    public SessionController(SessionService sessionService, CurrentUser currentUser) {
        this.sessionService = sessionService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<SessionDto.Response> start(@Valid @RequestBody SessionDto.StartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.start(currentUser.get(), request));
    }

    // 200 + session, or 204 when nothing is in progress
    @GetMapping("/active")
    public ResponseEntity<SessionDto.Response> active() {
        return sessionService.active(currentUser.get())
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<SessionDto.Summary> history(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search) {
        return sessionService.history(currentUser.get(), from, to, search);
    }

    @GetMapping("/dates")
    public List<SessionDto.DateCount> dates(@RequestParam String month) {
        return sessionService.datesInMonth(currentUser.get(), month);
    }

    @GetMapping("/export")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"workout-history.csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(sessionService.exportCsv(currentUser.get()));
    }

    @GetMapping("/{id}")
    public SessionDto.Response get(@PathVariable Long id) {
        return sessionService.get(currentUser.get(), id);
    }

    @PutMapping("/{id}/sets/{setId}")
    public SessionDto.SetResponse updateSet(@PathVariable Long id, @PathVariable Long setId,
                                            @Valid @RequestBody SessionDto.SetUpdate update) {
        return sessionService.updateSet(currentUser.get(), id, setId, update);
    }

    @PostMapping("/{id}/finish")
    public SessionDto.Response finish(@PathVariable Long id) {
        return sessionService.finish(currentUser.get(), id);
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<Void> abandon(@PathVariable Long id) {
        sessionService.abandon(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/to-routine")
    public ResponseEntity<PlanDto.Response> toRoutine(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.toRoutine(currentUser.get(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sessionService.delete(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }
}