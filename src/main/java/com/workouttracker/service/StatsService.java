package com.workouttracker.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.StatsDto;
import com.workouttracker.exception.ApiException;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.SessionExercise;
import com.workouttracker.model.SessionSet;
import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;
import com.workouttracker.model.WorkoutSession;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.WorkoutPlanRepository;
import com.workouttracker.repository.WorkoutSessionRepository;

// Every number here is calculated from COMPLETED sessions only. Planned workouts never count.
@Service
public class StatsService {

    private final WorkoutSessionRepository sessions;
    private final WorkoutPlanRepository plans;
    private final ExerciseRepository exercises;
    private final SettingsService settings;
    private final SessionService sessionService;

    public StatsService(WorkoutSessionRepository sessions, WorkoutPlanRepository plans, ExerciseRepository exercises,
                        SettingsService settings, SessionService sessionService) {
        this.sessions = sessions;
        this.plans = plans;
        this.exercises = exercises;
        this.settings = settings;
        this.sessionService = sessionService;
    }

    // ================= Public API =================

    @Transactional(readOnly = true)
    public StatsDto.Overview overview(User user) {
        List<WorkoutSession> done = completed(user);
        int goal = settings.view(user).weeklyGoal();
        int thisWeek = thisWeekCount(done);
        return new StatsDto.Overview(done.size(), thisWeek, goal, Math.max(0, goal - thisWeek),
                percent(thisWeek, goal), streak(done, goal), frequent(done, 5));
    }

    @Transactional(readOnly = true)
    public StatsDto.Dashboard dashboard(User user) {
        List<WorkoutSession> done = completed(user);
        int goal = settings.view(user).weeklyGoal();
        int thisWeek = thisWeekCount(done);
        LocalDate today = LocalDate.now();

        List<WorkoutPlan> scheduled = plans.findAllByUserAndDayOfWeekIsNotNull(user);
        WorkoutPlan todayPlan = scheduled.stream()
                .filter(p -> p.getDayOfWeek() == today.getDayOfWeek().getValue()).findFirst().orElse(null);

        // The program most scheduled days were copied from (null for fully custom schedules)
        String program = scheduled.stream().filter(p -> p.getSourceProgram() != null)
                .collect(Collectors.groupingBy(p -> p.getSourceProgram().getName(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        StatsDto.ActiveInfo active = sessions.findFirstByUserAndStatus(user, WorkoutSession.Status.IN_PROGRESS)
                .map(s -> new StatsDto.ActiveInfo(s.getId(), s.getName())).orElse(null);

        List<WorkoutSession> asc = ascending(done);
        List<StatsDto.Strength> strength = new ArrayList<>();
        for (StatsDto.Frequent f : frequent(done, 3)) {
            List<StatsDto.Point> points = points(asc, f.exerciseId());
            if (points.isEmpty()) continue;
            StatsDto.Point latest = points.get(points.size() - 1);
            StatsDto.Point previous = points.size() > 1 ? points.get(points.size() - 2) : null;
            strength.add(new StatsDto.Strength(f.exerciseId(), f.name(),
                    previous == null ? null : previous.weightKg(), previous == null ? null : previous.date(),
                    latest.weightKg(), latest.date()));
        }

        return new StatsDto.Dashboard(done.size(), thisWeek, goal, Math.max(0, goal - thisWeek),
                percent(thisWeek, goal), streak(done, goal), program,
                todayPlan == null ? null : new StatsDto.TodayPlan(todayPlan.getId(), todayPlan.getName(),
                        todayPlan.isRestDay(), todayPlan.getExercises().size()),
                active,
                done.stream().limit(5).map(sessionService::toSummary).toList(),
                week(user, today, done), strength);
    }

    // Monday-to-Sunday view of any week: what was planned and what was really completed
    @Transactional(readOnly = true)
    public List<StatsDto.DayStatus> week(User user, LocalDate anyDayInWeek) {
        return week(user, anyDayInWeek, completed(user));
    }

    @Transactional(readOnly = true)
    public List<StatsDto.ExerciseOption> exerciseOptions(User user) {
        Map<Long, String> byId = new TreeMap<>();
        for (WorkoutSession s : completed(user)) {
            for (SessionExercise e : s.getExercises()) {
                if (e.getSetList().stream().anyMatch(SessionSet::isCompleted)) {
                    byId.put(e.getExercise().getId(), e.getExercise().getName());
                }
            }
        }
        return byId.entrySet().stream()
                .map(e -> new StatsDto.ExerciseOption(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatsDto.ExerciseOption::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Transactional(readOnly = true)
    public StatsDto.ExerciseProgress exerciseProgress(User user, Long exerciseId) {
        Exercise exercise = exercises.findVisibleById(exerciseId, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exercise not found"));
        List<StatsDto.Point> points = points(ascending(completed(user)), exerciseId);

        StatsDto.Best personal = null;
        StatsDto.Point bestPoint = null;
        for (StatsDto.Point p : points) {
            if (bestPoint == null || isBetter(p.weightKg(), p.reps(), bestPoint.weightKg(), bestPoint.reps())) {
                bestPoint = p;
            }
        }
        if (bestPoint != null) personal = best(bestPoint);

        StatsDto.Best latest = points.isEmpty() ? null : best(points.get(points.size() - 1));
        StatsDto.Best previous = points.size() < 2 ? null : best(points.get(points.size() - 2));
        return new StatsDto.ExerciseProgress(exerciseId, exercise.getName(), personal, latest, previous, points);
    }

    // ================= Helpers =================

    private List<WorkoutSession> completed(User user) {
        return sessions.findAllByUserAndStatusOrderBySessionDateDescStartedAtDesc(user, WorkoutSession.Status.COMPLETED);
    }

    private List<WorkoutSession> ascending(List<WorkoutSession> descending) {
        List<WorkoutSession> copy = new ArrayList<>(descending);
        Collections.reverse(copy);
        return copy;
    }

    private LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private int thisWeekCount(List<WorkoutSession> done) {
        LocalDate monday = monday(LocalDate.now());
        return (int) done.stream().filter(s -> monday(s.getSessionDate()).equals(monday)).count();
    }

    private int percent(int done, int goal) {
        return goal <= 0 ? 0 : Math.min(100, done * 100 / goal);
    }

    // Consecutive Monday-Sunday weeks with at least `goal` completed sessions.
    // An unfinished current week does not break the streak; it counts once the goal is met.
    int streak(List<WorkoutSession> done, int goal) {
        Map<LocalDate, Long> perWeek = done.stream()
                .collect(Collectors.groupingBy(s -> monday(s.getSessionDate()), Collectors.counting()));
        LocalDate week = monday(LocalDate.now());
        int streak = 0;
        if (perWeek.getOrDefault(week, 0L) >= goal) streak++;
        week = week.minusWeeks(1);
        while (perWeek.getOrDefault(week, 0L) >= goal) {
            streak++;
            week = week.minusWeeks(1);
        }
        return streak;
    }

    private List<StatsDto.Frequent> frequent(List<WorkoutSession> done, int limit) {
        Map<Long, int[]> counts = new HashMap<>(); // id -> [sessions, sets]
        Map<Long, String> names = new HashMap<>();
        for (WorkoutSession s : done) {
            for (SessionExercise e : s.getExercises()) {
                int sets = (int) e.getSetList().stream().filter(SessionSet::isCompleted).count();
                if (sets == 0) continue;
                Long id = e.getExercise().getId();
                int[] c = counts.computeIfAbsent(id, k -> new int[2]);
                c[0]++;
                c[1] += sets;
                names.put(id, e.getExercise().getName());
            }
        }
        return counts.entrySet().stream()
                .map(en -> new StatsDto.Frequent(en.getKey(), names.get(en.getKey()), en.getValue()[0], en.getValue()[1]))
                .sorted(Comparator.comparingInt(StatsDto.Frequent::sessions).reversed()
                        .thenComparing(Comparator.comparingInt(StatsDto.Frequent::sets).reversed())
                        .thenComparing(StatsDto.Frequent::name))
                .limit(limit).toList();
    }

    // One point per session: the heaviest completed set (weight > 0), ties by reps. Oldest first.
    private List<StatsDto.Point> points(List<WorkoutSession> ascending, Long exerciseId) {
        List<StatsDto.Point> result = new ArrayList<>();
        for (WorkoutSession s : ascending) {
            SessionSet best = null;
            for (SessionExercise e : s.getExercises()) {
                if (!e.getExercise().getId().equals(exerciseId)) continue;
                for (SessionSet set : e.getSetList()) {
                    if (!set.isCompleted() || set.getActualWeight() == null || set.getActualWeight().signum() <= 0) continue;
                    if (best == null || isBetter(set.getActualWeight(), set.getActualReps(),
                            best.getActualWeight(), best.getActualReps())) {
                        best = set;
                    }
                }
            }
            if (best != null) {
                result.add(new StatsDto.Point(s.getSessionDate(), best.getActualWeight(), best.getActualReps()));
            }
        }
        return result;
    }

    private boolean isBetter(BigDecimal weight, Integer reps, BigDecimal otherWeight, Integer otherReps) {
        int cmp = weight.compareTo(otherWeight);
        if (cmp != 0) return cmp > 0;
        return (reps == null ? 0 : reps) > (otherReps == null ? 0 : otherReps);
    }

    private StatsDto.Best best(StatsDto.Point p) {
        return new StatsDto.Best(p.weightKg(), p.reps(), p.date());
    }

    private List<StatsDto.DayStatus> week(User user, LocalDate anyDay, List<WorkoutSession> done) {
        LocalDate monday = monday(anyDay);
        Map<Integer, WorkoutPlan> planByDay = new HashMap<>();
        plans.findAllByUserAndDayOfWeekIsNotNull(user).forEach(p -> planByDay.put(p.getDayOfWeek(), p));

        List<StatsDto.DayStatus> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            WorkoutPlan plan = planByDay.get(i + 1);
            List<StatsDto.Done> finished = done.stream().filter(s -> s.getSessionDate().equals(date))
                    .map(s -> new StatsDto.Done(s.getId(), s.getName())).toList();
            days.add(new StatsDto.DayStatus(i + 1,
                    DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH), date,
                    date.equals(LocalDate.now()), plan == null ? null : plan.getName(),
                    plan != null && plan.isRestDay(), finished));
        }
        return days;
    }
}