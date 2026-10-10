package com.workouttracker.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.dto.PlanDto;
import com.workouttracker.exception.ApiException;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.model.PlannedExercise;
import com.workouttracker.model.Program;
import com.workouttracker.model.ProgramDay;
import com.workouttracker.model.ProgramExercise;
import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.ProgramRepository;
import com.workouttracker.repository.WorkoutPlanRepository;

@Service
public class PlanService {

    private static final String DAY_TAKEN = "That day already has a planned workout";
    private static final String SCHEDULE_EXISTS = "You already have a weekly schedule";

    private record SampleDay(int day, String name, MuscleGroup category, List<String> exercises) {
    }

    // The example schedule from the project brief. Users can edit every part of it.
    private static final List<SampleDay> SAMPLE = List.of(
            new SampleDay(1, "Chest and Triceps", MuscleGroup.CHEST,
                    List.of("Bench Press", "Incline Dumbbell Press", "Tricep Pushdown", "Overhead Tricep Extension")),
            new SampleDay(2, "Back and Biceps", MuscleGroup.BACK,
                    List.of("Lat Pulldown", "Seated Row", "Bicep Curl", "Hammer Curl")),
            new SampleDay(3, "Legs", MuscleGroup.LEGS,
                    List.of("Squat", "Leg Press", "Romanian Deadlift", "Calf Raise")),
            new SampleDay(4, "Shoulders", MuscleGroup.SHOULDERS,
                    List.of("Shoulder Press", "Lateral Raise", "Rear Delt Fly", "Face Pull")),
            new SampleDay(5, "Upper Body", MuscleGroup.FULL_BODY,
                    List.of("Incline Dumbbell Press", "Seated Row", "Shoulder Press", "Bicep Curl")),
            new SampleDay(6, "Arms and Core", MuscleGroup.CORE,
                    List.of("Barbell Curl", "Close-Grip Bench Press", "Crunches", "Leg Raises")),
            new SampleDay(7, "Rest Day", null, List.of()));

    private final WorkoutPlanRepository planRepository;
    private final ExerciseRepository exerciseRepository;
    private final ProgramRepository programRepository;

    public PlanService(WorkoutPlanRepository planRepository, ExerciseRepository exerciseRepository,
                       ProgramRepository programRepository) {
        this.planRepository = planRepository;
        this.exerciseRepository = exerciseRepository;
        this.programRepository = programRepository;
    }

    // ================= Read =================

    @Transactional(readOnly = true)
    public List<PlanDto.Response> list(User user) {
        return planRepository.findAllByUser(user).stream()
                .sorted(Comparator
                        .comparingInt((WorkoutPlan p) -> p.getDayOfWeek() == null ? 8 : p.getDayOfWeek())
                        .thenComparing(WorkoutPlan::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanDto.Response get(User user, Long id) {
        return toResponse(findOwn(user, id));
    }

    // ================= Create / update / delete =================

    @Transactional
    public PlanDto.Response create(User user, PlanDto.Request request, boolean replace) {
        validate(request);
        // Resolve exercises first, so a bad request never deletes anything
        WorkoutPlan plan = new WorkoutPlan(user, cleanName(request), request.category(),
                request.dayOfWeek(), request.restDay());
        applyItems(user, plan, request);

        claimDay(user, request.dayOfWeek(), replace, null);
        return toResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanDto.Response update(User user, Long id, PlanDto.Request request, boolean replace) {
        WorkoutPlan plan = findOwn(user, id);
        validate(request);

        claimDay(user, request.dayOfWeek(), replace, id);
        plan.setName(cleanName(request));
        plan.setCategory(request.category());
        plan.setDayOfWeek(request.dayOfWeek());
        plan.setRestDay(request.restDay());
        applyItems(user, plan, request);
        return toResponse(plan); // saved automatically when the transaction ends
    }

    @Transactional
    public void delete(User user, Long id) {
        planRepository.delete(findOwn(user, id));
    }

    // ================= Duplicate / copy / move =================

    // A copy that is NOT on the week (a new saved routine)
    @Transactional
    public PlanDto.Response duplicate(User user, Long id) {
        WorkoutPlan source = findOwn(user, id);
        if (source.isRestDay()) {
            throw bad("Rest days cannot be duplicated");
        }
        String baseName = source.getName().length() > 93 ? source.getName().substring(0, 93) : source.getName();
        WorkoutPlan copy = new WorkoutPlan(user, baseName + " (copy)", source.getCategory(), null, false);
        copyItems(source, copy);
        return toResponse(planRepository.save(copy));
    }

    // Copies a workout to another weekday. The original stays where it is.
    @Transactional
    public PlanDto.Response copyToDay(User user, Long id, int day, boolean replace) {
        checkDay(day);
        WorkoutPlan source = findOwn(user, id);
        if (source.getDayOfWeek() != null && source.getDayOfWeek() == day) {
            throw bad("Choose a different day");
        }
        claimDay(user, day, replace, null);

        WorkoutPlan copy = new WorkoutPlan(user, source.getName(), source.getCategory(), day, source.isRestDay());
        copy.setSourceProgram(source.getSourceProgram());
        copyItems(source, copy);
        return toResponse(planRepository.save(copy));
    }

    // Puts a plan on a weekday, or takes it off the week when day is null
    @Transactional
    public PlanDto.Response moveToDay(User user, Long id, Integer day, boolean replace) {
        WorkoutPlan plan = findOwn(user, id);
        if (day == null) {
            if (plan.isRestDay()) {
                throw bad("A rest day must stay on a day. Delete it instead.");
            }
            plan.setDayOfWeek(null);
            return toResponse(plan);
        }
        checkDay(day);
        claimDay(user, day, replace, id);
        plan.setDayOfWeek(day);
        return toResponse(plan);
    }

    // ================= Programs and sample schedule =================

    // "Use This Plan": copies the template into the user's own weekly schedule.
    // The template itself is never modified.
    @Transactional
    public List<PlanDto.Response> fromProgram(User user, Long programId, boolean replace) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        clearSchedule(user, replace);

        List<PlanDto.Response> created = new ArrayList<>();
        for (ProgramDay day : program.getDays()) {
            WorkoutPlan plan = new WorkoutPlan(user, day.getName(), null, day.getDayOfWeek(), day.isRestDay());
            plan.setSourceProgram(program);
            for (ProgramExercise item : day.getExercises()) {
                plan.addExercise(new PlannedExercise(item.getExercise(), item.getSortOrder(), item.getSets(),
                        item.getReps(), item.getDurationSeconds(), null, item.getRestSeconds()));
            }
            created.add(toResponse(planRepository.save(plan)));
        }
        return created;
    }

    @Transactional
    public List<PlanDto.Response> sampleSchedule(User user, boolean replace) {
        clearSchedule(user, replace);

        List<PlanDto.Response> created = new ArrayList<>();
        for (SampleDay sample : SAMPLE) {
            WorkoutPlan plan = new WorkoutPlan(user, sample.name(), sample.category(),
                    sample.day(), sample.exercises().isEmpty());
            int order = 1;
            for (String exerciseName : sample.exercises()) {
                Exercise exercise = exerciseRepository.findByNameIgnoreCaseAndUserIsNull(exerciseName).orElse(null);
                if (exercise == null) {
                    continue; // skip anything missing from the library
                }
                plan.addExercise(new PlannedExercise(exercise, order++, 3, 10, null, null, 60));
            }
            created.add(toResponse(planRepository.save(plan)));
        }
        return created;
    }

    // ================= Helpers =================

    // Own plan only. Someone else's plan looks exactly like a missing one (404).
    private WorkoutPlan findOwn(User user, Long id) {
        return planRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workout plan not found"));
    }

    private ApiException bad(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private void checkDay(int day) {
        if (day < 1 || day > 7) {
            throw bad("Day must be between Monday and Sunday");
        }
    }

    private void validate(PlanDto.Request request) {
        boolean hasExercises = request.exercises() != null && !request.exercises().isEmpty();
        if (request.restDay()) {
            if (request.dayOfWeek() == null) {
                throw bad("A rest day needs a day of the week");
            }
            if (hasExercises) {
                throw bad("A rest day cannot have exercises");
            }
        } else {
            if (request.name() == null || request.name().isBlank()) {
                throw bad("Workout name is required");
            }
            if (!hasExercises) {
                throw bad("Add at least one exercise");
            }
        }
    }

    private String cleanName(PlanDto.Request request) {
        return request.restDay() ? "Rest Day" : request.name().trim();
    }

    // Replaces the exercises of a plan with the ones from the request
    private void applyItems(User user, WorkoutPlan plan, PlanDto.Request request) {
        plan.getExercises().clear();
        if (request.restDay() || request.exercises() == null) {
            return;
        }
        int order = 1;
        for (PlanDto.Item item : request.exercises()) {
            boolean hasReps = item.reps() != null;
            boolean hasTime = item.durationSeconds() != null;
            if (hasReps == hasTime) {
                throw bad("Each exercise needs reps or a duration (not both)");
            }
            Exercise exercise = exerciseRepository.findVisibleById(item.exerciseId(), user)
                    .orElseThrow(() -> bad("One of the selected exercises does not exist"));
            BigDecimal weight = item.targetWeight() == null
                    ? null : item.targetWeight().setScale(2, RoundingMode.HALF_UP);
            plan.addExercise(new PlannedExercise(exercise, order++, item.sets(), item.reps(),
                    item.durationSeconds(), weight, item.restSeconds()));
        }
    }

    private void copyItems(WorkoutPlan source, WorkoutPlan target) {
        for (PlannedExercise item : source.getExercises()) {
            target.addExercise(new PlannedExercise(item.getExercise(), item.getSortOrder(), item.getSets(),
                    item.getReps(), item.getDurationSeconds(), item.getTargetWeight(), item.getRestSeconds()));
        }
    }

    // Makes sure the weekday is free. If another plan is there: 409, or delete it when replace=true.
    private void claimDay(User user, Integer day, boolean replace, Long ignorePlanId) {
        if (day == null) {
            return;
        }
        Optional<WorkoutPlan> existing = planRepository.findByUserAndDayOfWeek(user, day);
        if (existing.isPresent() && !existing.get().getId().equals(ignorePlanId)) {
            if (!replace) {
                throw new ApiException(HttpStatus.CONFLICT, DAY_TAKEN);
            }
            planRepository.delete(existing.get());
            planRepository.flush(); // delete NOW, before the new row is inserted (unique constraint)
        }
    }

    // Removes every scheduled plan (saved routines that are not on the week stay)
    private void clearSchedule(User user, boolean replace) {
        List<WorkoutPlan> scheduled = planRepository.findAllByUserAndDayOfWeekIsNotNull(user);
        if (scheduled.isEmpty()) {
            return;
        }
        if (!replace) {
            throw new ApiException(HttpStatus.CONFLICT, SCHEDULE_EXISTS);
        }
        planRepository.deleteAll(scheduled);
        planRepository.flush();
    }

    private PlanDto.Response toResponse(WorkoutPlan plan) {
        Integer day = plan.getDayOfWeek();
        String dayName = day == null ? null : DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        MuscleGroup category = plan.getCategory();

        List<PlanDto.ItemResponse> items = plan.getExercises().stream()
                .map(item -> new PlanDto.ItemResponse(
                        item.getExercise().getId(), item.getExercise().getName(),
                        item.getExercise().getMuscleGroup().getLabel(),
                        item.getSets(), item.getReps(), item.getDurationSeconds(),
                        item.getTargetWeight(), item.getRestSeconds()))
                .toList();

        return new PlanDto.Response(plan.getId(), plan.getName(),
                category == null ? null : category.name(),
                category == null ? null : category.getLabel(),
                day, dayName, plan.isRestDay(),
                plan.getSourceProgram() == null ? null : plan.getSourceProgram().getName(),
                items);
    }
}