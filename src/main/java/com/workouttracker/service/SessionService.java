package com.workouttracker.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.PlanDto;
import com.workouttracker.dto.SessionDto;
import com.workouttracker.exception.ApiException;
import com.workouttracker.model.PlannedExercise;
import com.workouttracker.model.SessionExercise;
import com.workouttracker.model.SessionSet;
import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;
import com.workouttracker.model.WorkoutSession;
import static com.workouttracker.model.WorkoutSession.Status.COMPLETED;
import static com.workouttracker.model.WorkoutSession.Status.IN_PROGRESS;
import com.workouttracker.repository.WorkoutPlanRepository;
import com.workouttracker.repository.WorkoutSessionRepository;

@Service
public class SessionService {

    private final WorkoutSessionRepository sessions;
    private final WorkoutPlanRepository plans;
    private final PlanService planService;

    public SessionService(WorkoutSessionRepository sessions, WorkoutPlanRepository plans, PlanService planService) {
        this.sessions = sessions;
        this.plans = plans;
        this.planService = planService;
    }

    // ================= Start / read =================

    @Transactional
    public SessionDto.Response start(User user, SessionDto.StartRequest request) {
        // Same token again (double click / retry) -> the same session, no duplicate
        Optional<WorkoutSession> same = sessions.findByUserAndClientToken(user, request.clientToken());
        if (same.isPresent()) {
            return toResponse(same.get());
        }
        if (sessions.findFirstByUserAndStatus(user, IN_PROGRESS).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You already have an active workout. Finish or discard it first.");
        }
        WorkoutPlan plan = plans.findByIdAndUser(request.planId(), user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workout plan not found"));
        if (plan.isRestDay() || plan.getExercises().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This workout has no exercises to start");
        }

        WorkoutSession session = new WorkoutSession(user, plan.getId(), plan.getName(),
                request.clientToken(), LocalDate.now());
        for (PlannedExercise planned : plan.getExercises()) {
            SessionExercise exercise = new SessionExercise(planned.getExercise(), planned.getSortOrder(),
                    planned.getSets(), planned.getReps(), planned.getDurationSeconds(),
                    planned.getTargetWeight(), planned.getRestSeconds());
            for (int number = 1; number <= planned.getSets(); number++) {
                exercise.addSet(new SessionSet(number));
            }
            session.addExercise(exercise);
        }
        return toResponse(sessions.save(session));
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto.Response> active(User user) {
        return sessions.findFirstByUserAndStatus(user, IN_PROGRESS).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionDto.Response get(User user, Long id) {
        return toResponse(own(user, id));
    }

    // ================= Logging =================

    @Transactional
    public SessionDto.SetResponse updateSet(User user, Long sessionId, Long setId, SessionDto.SetUpdate update) {
        WorkoutSession session = own(user, sessionId);
        requireActive(session);

        SessionSet set = session.getExercises().stream()
                .flatMap(e -> e.getSetList().stream())
                .filter(s -> s.getId().equals(setId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Set not found"));

        boolean timed = set.getSessionExercise().getDurationSeconds() != null;
        if (update.completed()) {
            if (timed && update.actualDurationSeconds() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the seconds you completed");
            }
            if (!timed && update.actualReps() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Enter the reps you completed");
            }
        }
        set.apply(timed ? null : update.actualReps(), timed ? update.actualDurationSeconds() : null,
                update.actualWeight(), update.completed());
        return toSet(set);
    }

    @Transactional
    public SessionDto.Response finish(User user, Long id) {
        WorkoutSession session = own(user, id);
        requireActive(session);
        boolean anyDone = session.getExercises().stream()
                .anyMatch(e -> e.getSetList().stream().anyMatch(SessionSet::isCompleted));
        if (!anyDone) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Complete at least one set before finishing");
        }
        session.complete(Instant.now());
        return toResponse(session);
    }

    // Discard: the session is kept as ABANDONED and never counted as a workout
    @Transactional
    public void abandon(User user, Long id) {
        WorkoutSession session = own(user, id);
        requireActive(session);
        session.abandon(Instant.now());
    }

    @Transactional
    public void delete(User user, Long id) {
        WorkoutSession session = own(user, id);
        if (session.getStatus() == IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "Finish or discard the active workout first");
        }
        sessions.delete(session);
    }

    // ================= History =================

    @Transactional(readOnly = true)
    public List<SessionDto.Summary> history(User user, LocalDate from, LocalDate to, String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return sessions.findAllByUserAndStatusOrderBySessionDateDescStartedAtDesc(user, COMPLETED).stream()
                .filter(s -> from == null || !s.getSessionDate().isBefore(from))
                .filter(s -> to == null || !s.getSessionDate().isAfter(to))
                .filter(s -> term.isEmpty()
                        || s.getName().toLowerCase().contains(term)
                        || s.getExercises().stream().anyMatch(e -> e.getExerciseName().toLowerCase().contains(term)))
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionDto.DateCount> datesInMonth(User user, String month) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(month);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Month must look like 2026-10");
        }
        Map<LocalDate, Long> counts = sessions
                .findAllByUserAndStatusOrderBySessionDateDescStartedAtDesc(user, COMPLETED).stream()
                .filter(s -> YearMonth.from(s.getSessionDate()).equals(ym))
                .collect(Collectors.groupingBy(WorkoutSession::getSessionDate, TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> new SessionDto.DateCount(e.getKey(), e.getValue().intValue())).toList();
    }

    // "Reuse a previous workout": a new, unscheduled routine using the weights actually lifted
    @Transactional
    public PlanDto.Response toRoutine(User user, Long id) {
        WorkoutSession session = own(user, id);
        if (session.getStatus() != COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only finished workouts can be reused");
        }
        String base = session.getName().length() > 80 ? session.getName().substring(0, 80) : session.getName();
        WorkoutPlan plan = new WorkoutPlan(user, base + " (from history)", null, null, false);
        for (SessionExercise e : session.getExercises()) {
            BigDecimal weight = e.getSetList().stream().filter(SessionSet::isCompleted)
                    .map(SessionSet::getActualWeight).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(e.getTargetWeight());
            plan.addExercise(new PlannedExercise(e.getExercise(), e.getSortOrder(), e.getPlannedSets(),
                    e.getReps(), e.getDurationSeconds(), weight, e.getRestSeconds()));
        }
        plans.save(plan);
        return planService.get(user, plan.getId());
    }

    @Transactional(readOnly = true)
    public String exportCsv(User user) {
        List<WorkoutSession> done = new ArrayList<>(
                sessions.findAllByUserAndStatusOrderBySessionDateDescStartedAtDesc(user, COMPLETED));
        Collections.reverse(done);
        StringBuilder csv = new StringBuilder("date,workout,exercise,set,reps,seconds,weight_kg\n");
        for (WorkoutSession s : done) {
            for (SessionExercise e : s.getExercises()) {
                for (SessionSet set : e.getSetList()) {
                    if (!set.isCompleted()) continue;
                    csv.append(s.getSessionDate()).append(',').append(cell(s.getName())).append(',')
                       .append(cell(e.getExerciseName())).append(',').append(set.getSetNumber()).append(',')
                       .append(set.getActualReps() == null ? "" : set.getActualReps()).append(',')
                       .append(set.getActualDurationSeconds() == null ? "" : set.getActualDurationSeconds()).append(',')
                       .append(set.getActualWeight() == null ? "" : set.getActualWeight().toPlainString()).append('\n');
                }
            }
        }
        return csv.toString();
    }

    // ================= Helpers =================

    // Escapes quotes and neutralises spreadsheet formulas (=, +, -, @) in text cells
    private String cell(String text) {
        String safe = text;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private WorkoutSession own(User user, Long id) {
        return sessions.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workout session not found"));
    }

    private void requireActive(WorkoutSession session) {
        if (session.getStatus() != IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "This workout is no longer active");
        }
    }

    private SessionDto.SetResponse toSet(SessionSet s) {
        return new SessionDto.SetResponse(s.getId(), s.getSetNumber(), s.getActualReps(),
                s.getActualDurationSeconds(), s.getActualWeight(), s.isCompleted());
    }

    private long minutes(WorkoutSession s) {
        return s.getFinishedAt() == null ? 0 : Duration.between(s.getStartedAt(), s.getFinishedAt()).toMinutes();
    }

    private int[] counts(WorkoutSession s) {
        int total = 0;
        int done = 0;
        for (SessionExercise e : s.getExercises()) {
            for (SessionSet set : e.getSetList()) {
                total++;
                if (set.isCompleted()) done++;
            }
        }
        return new int[]{done, total};
    }

    public SessionDto.Summary toSummary(WorkoutSession s) {
        int[] c = counts(s);
        List<String> names = s.getExercises().stream()
                .filter(e -> e.getSetList().stream().anyMatch(SessionSet::isCompleted))
                .map(SessionExercise::getExerciseName).toList();
        return new SessionDto.Summary(s.getId(), s.getName(), s.getSessionDate(), s.getStartedAt(),
                minutes(s), c[0], c[1], names);
    }

    private SessionDto.Response toResponse(WorkoutSession s) {
        int[] c = counts(s);
        List<SessionDto.ExerciseResponse> exercises = s.getExercises().stream()
                .map(e -> new SessionDto.ExerciseResponse(e.getId(), e.getExercise().getId(), e.getExerciseName(),
                        e.getExercise().getDescription(), e.getPlannedSets(), e.getReps(), e.getDurationSeconds(),
                        e.getTargetWeight(), e.getRestSeconds(),
                        e.getSetList().stream().map(this::toSet).toList()))
                .toList();
        return new SessionDto.Response(s.getId(), s.getSourcePlanId(), s.getName(), s.getStatus().name(),
                s.getSessionDate(), s.getStartedAt(), s.getFinishedAt(),
                s.getFinishedAt() == null ? null : minutes(s), c[0], c[1], exercises);
    }
}