package com.workouttracker.config;

import com.workouttracker.model.Exercise;
import com.workouttracker.model.Program;
import com.workouttracker.model.ProgramCategory;
import com.workouttracker.model.ProgramDay;
import com.workouttracker.model.ProgramExercise;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.ProgramRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.workouttracker.model.MuscleGroup.*;

// Adds the built-in exercises and program templates if they are missing.
// Safe to run on every startup: existing rows are skipped, nothing is overwritten.
@Component
public class DataSeeder implements ApplicationRunner {

    private static final int MON = 1, TUE = 2, WED = 3, THU = 4, FRI = 5, SAT = 6, SUN = 7;

    private final ExerciseRepository exerciseRepository;
    private final ProgramRepository programRepository;

    public DataSeeder(ExerciseRepository exerciseRepository, ProgramRepository programRepository) {
        this.exerciseRepository = exerciseRepository;
        this.programRepository = programRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedExercises();
        seedPrograms();
    }

    // ================= Exercises =================

    private void seedExercises() {
        exercise("Bench Press", CHEST, "Barbell", "Lie on a flat bench and press a barbell up from chest level.");
        exercise("Incline Dumbbell Press", CHEST, "Dumbbells", "Press dumbbells upward while lying on an incline bench.");
        exercise("Dumbbell Fly", CHEST, "Dumbbells", "With slightly bent elbows, open and close the arms in an arc over the chest.");
        exercise("Cable Crossover", CHEST, "Cable machine", "Bring the cable handles together in front of the chest.");
        exercise("Chest Press Machine", CHEST, "Machine", "Push the handles forward from chest height on a seated machine.");

        exercise("Lat Pulldown", BACK, "Cable machine", "Pull a bar down toward the upper chest from overhead while seated.");
        exercise("Seated Row", BACK, "Cable machine", "Pull a handle toward the torso while seated with an upright back.");
        exercise("Barbell Row", BACK, "Barbell", "Hinge forward at the hips and pull a barbell toward the lower ribs.");
        exercise("One-Arm Dumbbell Row", BACK, "Dumbbell", "Supported on a bench, pull a dumbbell up toward the hip with one arm.");
        exercise("Deadlift", BACK, "Barbell", "Lift a barbell from the floor to hip height by extending the hips and knees.");

        exercise("Shoulder Press", SHOULDERS, "Dumbbells", "Press dumbbells overhead from shoulder height.");
        exercise("Lateral Raise", SHOULDERS, "Dumbbells", "Raise dumbbells out to the sides to shoulder height.");
        exercise("Front Raise", SHOULDERS, "Dumbbells", "Raise dumbbells forward to shoulder height.");
        exercise("Face Pull", SHOULDERS, "Cable machine", "Pull a rope attachment toward the face with the elbows high.");
        exercise("Rear Delt Fly", SHOULDERS, "Dumbbells", "Bent over, raise dumbbells out to the sides.");

        exercise("Bicep Curl", BICEPS, "Dumbbells", "Curl dumbbells from hip level up toward the shoulders.");
        exercise("Hammer Curl", BICEPS, "Dumbbells", "Curl dumbbells with the palms facing each other.");
        exercise("Barbell Curl", BICEPS, "Barbell", "Curl a barbell toward the chest with an underhand grip.");
        exercise("Preacher Curl", BICEPS, "EZ bar", "Curl an EZ bar with the upper arms supported on a preacher bench.");

        exercise("Tricep Pushdown", TRICEPS, "Cable machine", "Push a cable attachment down by extending the elbows.");
        exercise("Overhead Tricep Extension", TRICEPS, "Dumbbell", "Lower a dumbbell behind the head, then extend the elbows to raise it.");
        exercise("Close-Grip Bench Press", TRICEPS, "Barbell", "Bench press with the hands closer together than usual.");

        exercise("Squat", LEGS, "Barbell", "Lower the hips with a barbell on the upper back, then stand back up.");
        exercise("Goblet Squat", LEGS, "Dumbbell", "Hold a dumbbell at the chest and squat down and up.");
        exercise("Leg Press", LEGS, "Machine", "Push a weighted platform away with the feet while seated.");
        exercise("Romanian Deadlift", LEGS, "Barbell", "Hinge at the hips with nearly straight legs, then stand tall.");
        exercise("Leg Curl", LEGS, "Machine", "Curl the heels toward the glutes against resistance.");
        exercise("Leg Extension", LEGS, "Machine", "Extend the knees against resistance while seated.");
        exercise("Calf Raise", LEGS, "Machine", "Rise onto the toes against resistance, then lower with control.");
        exercise("Glute Bridge", LEGS, "Bodyweight", "Lying on the back, lift the hips by pressing through the feet.");

        exercise("Crunches", CORE, "Bodyweight", "Lying on the back, curl the shoulders toward the hips.");
        exercise("Leg Raises", CORE, "Bodyweight", "Lying on the back, raise the legs and lower them with control.");
        exercise("Plank", CORE, "Bodyweight", "Hold a straight line from head to heels, supported on forearms and toes.");
        exercise("Bicycle Crunches", CORE, "Bodyweight", "Alternate bringing opposite elbow and knee together while lying down.");
        exercise("Reverse Crunches", CORE, "Bodyweight", "Lying on the back, curl the knees toward the chest and lift the hips.");
        exercise("Russian Twist", CORE, "Bodyweight", "Seated and leaning back slightly, rotate the torso side to side.");

        exercise("Walking", CARDIO, "Treadmill or outdoors", "Walk at a steady, comfortable pace.");
        exercise("Running", CARDIO, "Treadmill or outdoors", "Run at a steady, comfortable pace.");
        exercise("Cycling", CARDIO, "Bike or stationary bike", "Pedal at a steady pace.");
        exercise("Rowing Machine", CARDIO, "Rowing machine", "Row at a steady pace using the legs, back, and arms.");
        exercise("Elliptical", CARDIO, "Elliptical machine", "Move at a steady pace on the elliptical.");
        exercise("Jump Rope", CARDIO, "Jump rope", "Skip rope at a steady rhythm.");

        exercise("Burpees", FULL_BODY, "Bodyweight", "Drop to a push-up position, return to standing, and jump.");
        exercise("Kettlebell Swing", FULL_BODY, "Kettlebell", "Swing a kettlebell to chest height by driving the hips forward.");
        exercise("Dumbbell Thruster", FULL_BODY, "Dumbbells", "Squat with dumbbells at the shoulders, then press them overhead as you stand.");
        exercise("Mountain Climbers", FULL_BODY, "Bodyweight", "From a plank position, alternately drive the knees toward the chest.");

        exercise("Push-Ups", CALISTHENICS, "Bodyweight", "Lower the chest toward the floor from a plank position and press back up.");
        exercise("Pull-Ups", CALISTHENICS, "Pull-up bar", "Hang from a bar and pull the chin above it.");
        exercise("Dips", CALISTHENICS, "Parallel bars", "Lower and raise the body between parallel bars by bending the elbows.");
        exercise("Bodyweight Squats", CALISTHENICS, "Bodyweight", "Squat down and stand back up without added weight.");
        exercise("Lunges", CALISTHENICS, "Bodyweight", "Step forward, lower the back knee toward the floor, then return.");
        exercise("Inverted Row", CALISTHENICS, "Bar or rings", "Hanging below a low bar, pull the chest toward it.");
        exercise("Pike Push-Ups", CALISTHENICS, "Bodyweight", "With the hips raised, lower the head toward the floor and press back up.");

        exercise("Hamstring Stretch", FLEXIBILITY, "Bodyweight", "Extend one leg and gently reach toward the toes.");
        exercise("Quad Stretch", FLEXIBILITY, "Bodyweight", "Standing, gently pull one heel toward the glutes.");
        exercise("Hip Flexor Stretch", FLEXIBILITY, "Bodyweight", "In a lunge position, gently shift the hips forward.");
        exercise("Cat-Cow", FLEXIBILITY, "Bodyweight", "On hands and knees, alternate arching and rounding the back.");
        exercise("Child's Pose", FLEXIBILITY, "Bodyweight", "Kneel and sit back with the arms extended forward.");
        exercise("Chest Doorway Stretch", FLEXIBILITY, "Door frame", "Place a forearm on a door frame and gently turn away.");
        exercise("Cross-Body Shoulder Stretch", FLEXIBILITY, "Bodyweight", "Pull one arm across the chest with the other arm.");
        exercise("Thoracic Rotation", FLEXIBILITY, "Bodyweight", "On hands and knees, rotate the upper back with one hand behind the head.");
    }

    private void exercise(String name, com.workouttracker.model.MuscleGroup group, String equipment, String description) {
        if (!exerciseRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            exerciseRepository.save(new Exercise(name, group, equipment, description));
        }
    }

    // ================= Programs =================

    private record Item(String exercise, int sets, Integer reps, Integer seconds, int rest) {
    }

    private record DaySpec(int dayOfWeek, String name, List<Item> items) {
    }

    // Strength item: sets x reps
    private static Item reps(String exercise, int sets, int reps, int rest) {
        return new Item(exercise, sets, reps, null, rest);
    }

    // Timed item: sets x seconds
    private static Item timed(String exercise, int sets, int seconds, int rest) {
        return new Item(exercise, sets, null, seconds, rest);
    }

    private static DaySpec day(int dayOfWeek, String name, Item... items) {
        return new DaySpec(dayOfWeek, name, List.of(items));
    }

    private static DaySpec rest(int dayOfWeek) {
        return new DaySpec(dayOfWeek, "Rest Day", List.of());
    }

    private void seedPrograms() {
        Map<String, Exercise> library = new HashMap<>();
        for (Exercise e : exerciseRepository.findAllByUserIsNull()) {
            library.put(e.getName().toLowerCase(), e);
        }

        program(library, "Bodybuilding Muscle Split", ProgramCategory.BODYBUILDING,
                "A five-day split that trains one or two muscle groups per session with classic strength exercises. A starting point you can adjust.",
                day(MON, "Chest and Triceps",
                        reps("Bench Press", 4, 8, 90), reps("Incline Dumbbell Press", 3, 10, 90),
                        reps("Dumbbell Fly", 3, 12, 60), reps("Tricep Pushdown", 3, 12, 60),
                        reps("Overhead Tricep Extension", 3, 12, 60)),
                day(TUE, "Back and Biceps",
                        reps("Lat Pulldown", 4, 10, 90), reps("Seated Row", 3, 10, 90),
                        reps("One-Arm Dumbbell Row", 3, 10, 60), reps("Bicep Curl", 3, 12, 60),
                        reps("Hammer Curl", 3, 12, 60)),
                day(WED, "Legs",
                        reps("Squat", 4, 8, 120), reps("Leg Press", 3, 10, 90),
                        reps("Romanian Deadlift", 3, 10, 90), reps("Leg Curl", 3, 12, 60),
                        reps("Calf Raise", 4, 15, 60)),
                day(THU, "Shoulders",
                        reps("Shoulder Press", 4, 8, 90), reps("Lateral Raise", 3, 12, 60),
                        reps("Front Raise", 3, 12, 60), reps("Rear Delt Fly", 3, 12, 60),
                        reps("Face Pull", 3, 15, 60)),
                day(FRI, "Upper Body",
                        reps("Incline Dumbbell Press", 3, 10, 90), reps("Seated Row", 3, 10, 90),
                        reps("Shoulder Press", 3, 10, 90), reps("Bicep Curl", 3, 12, 60),
                        reps("Tricep Pushdown", 3, 12, 60)),
                rest(SAT), rest(SUN));

        program(library, "Balanced Weight Loss Plan", ProgramCategory.WEIGHT_LOSS,
                "Combines full-body strength sessions with walking, cycling, and bodyweight conditioning to build a regular training habit. It does not promise specific results.",
                day(MON, "Full Body Strength A",
                        reps("Goblet Squat", 3, 12, 60), reps("Push-Ups", 3, 10, 60),
                        reps("One-Arm Dumbbell Row", 3, 12, 60), timed("Plank", 3, 30, 45)),
                day(TUE, "Walking", timed("Walking", 1, 1800, 0)),
                day(WED, "Full Body Strength B",
                        reps("Romanian Deadlift", 3, 10, 75), reps("Lunges", 3, 10, 60),
                        reps("Lat Pulldown", 3, 12, 60), reps("Shoulder Press", 3, 12, 60),
                        reps("Glute Bridge", 3, 15, 45)),
                day(THU, "Cycling", timed("Cycling", 1, 1800, 0)),
                day(FRI, "Bodyweight Conditioning",
                        reps("Bodyweight Squats", 3, 15, 45), timed("Mountain Climbers", 3, 30, 45),
                        reps("Burpees", 3, 8, 60), timed("Jump Rope", 3, 60, 45)),
                day(SAT, "Long Walk", timed("Walking", 1, 2700, 0)),
                rest(SUN));

        program(library, "Calisthenics Foundations", ProgramCategory.CALISTHENICS,
                "A bodyweight-focused plan built on push, pull, and leg movements. Choose easier or harder variations to suit your level.",
                day(MON, "Push",
                        reps("Push-Ups", 4, 10, 60), reps("Dips", 3, 8, 90), reps("Pike Push-Ups", 3, 8, 60)),
                day(TUE, "Pull and Core",
                        reps("Pull-Ups", 4, 6, 90), reps("Inverted Row", 3, 10, 60), timed("Plank", 3, 45, 45)),
                day(WED, "Legs",
                        reps("Bodyweight Squats", 4, 15, 60), reps("Lunges", 3, 12, 60),
                        reps("Glute Bridge", 3, 15, 45)),
                rest(THU),
                day(FRI, "Full Body",
                        reps("Push-Ups", 3, 10, 60), reps("Pull-Ups", 3, 6, 90), reps("Dips", 3, 8, 90),
                        reps("Bodyweight Squats", 3, 15, 60), timed("Plank", 3, 45, 45)),
                rest(SAT), rest(SUN));

        program(library, "Core Strength Basics", ProgramCategory.ABS_CORE,
                "Three short core sessions per week using crunches, leg raises, and planks. Move with control and stop if something feels wrong.",
                day(MON, "Core A",
                        reps("Crunches", 3, 15, 45), reps("Leg Raises", 3, 12, 45), timed("Plank", 3, 30, 45)),
                rest(TUE),
                day(WED, "Core B",
                        reps("Bicycle Crunches", 3, 20, 45), reps("Reverse Crunches", 3, 12, 45),
                        reps("Russian Twist", 3, 20, 45)),
                rest(THU),
                day(FRI, "Core C",
                        timed("Plank", 3, 45, 45), reps("Leg Raises", 3, 12, 45),
                        reps("Crunches", 3, 15, 45), reps("Bicycle Crunches", 3, 20, 45)),
                rest(SAT), rest(SUN));

        program(library, "Cardio Training Basics", ProgramCategory.CARDIO,
                "A mix of walking, running, and cycling sessions at a steady, comfortable pace, with rest days in between.",
                day(MON, "Walking", timed("Walking", 1, 1800, 0)),
                day(TUE, "Easy Run", timed("Running", 1, 1200, 0)),
                rest(WED),
                day(THU, "Cycling", timed("Cycling", 1, 1800, 0)),
                day(FRI, "Cardio Mix", timed("Rowing Machine", 1, 900, 60), timed("Elliptical", 1, 900, 0)),
                day(SAT, "Long Walk", timed("Walking", 1, 2700, 0)),
                rest(SUN));

        program(library, "Flexibility and Mobility Routine", ProgramCategory.FLEXIBILITY,
                "Gentle stretching and mobility routines for the lower body, upper body, and full body. Stretch only to mild tension, never pain.",
                day(MON, "Lower Body Mobility",
                        timed("Hamstring Stretch", 2, 30, 15), timed("Quad Stretch", 2, 30, 15),
                        timed("Hip Flexor Stretch", 2, 30, 15)),
                rest(TUE),
                day(WED, "Upper Body Mobility",
                        timed("Cat-Cow", 2, 30, 15), reps("Thoracic Rotation", 2, 10, 15),
                        timed("Chest Doorway Stretch", 2, 30, 15), timed("Cross-Body Shoulder Stretch", 2, 30, 15)),
                rest(THU),
                day(FRI, "Full Body Stretch",
                        timed("Child's Pose", 2, 45, 15), timed("Hamstring Stretch", 2, 30, 15),
                        timed("Hip Flexor Stretch", 2, 30, 15), timed("Cat-Cow", 2, 30, 15)),
                rest(SAT),
                day(SUN, "Recovery",
                        timed("Child's Pose", 1, 60, 0), timed("Cat-Cow", 1, 60, 0),
                        timed("Cross-Body Shoulder Stretch", 2, 30, 15)));
    }

    private void program(Map<String, Exercise> library, String name, ProgramCategory category,
                         String description, DaySpec... days) {
        if (programRepository.existsByName(name)) {
            return;
        }

        Program program = new Program(name, category, description);
        for (DaySpec spec : days) {
            ProgramDay day = new ProgramDay(spec.dayOfWeek(), spec.name(), spec.items().isEmpty());
            int order = 1;
            for (Item item : spec.items()) {
                Exercise exercise = library.get(item.exercise().toLowerCase());
                if (exercise == null) {
                    throw new IllegalStateException("Seed data uses an unknown exercise: " + item.exercise());
                }
                day.addExercise(new ProgramExercise(exercise, order++, item.sets(),
                        item.reps(), item.seconds(), item.rest()));
            }
            program.addDay(day);
        }
        programRepository.save(program);
    }
}