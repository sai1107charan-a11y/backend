package com.workouttracker;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.config.DataSeeder;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.PlannedExercise;
import com.workouttracker.model.SessionExercise;
import com.workouttracker.model.SessionSet;
import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;
import com.workouttracker.model.WorkoutSession;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.UserRepository;
import com.workouttracker.repository.WorkoutPlanRepository;
import com.workouttracker.repository.WorkoutSessionRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsApiTest {

    static final String ALICE = "alice@mail.com";
    static final String BOB = "bob@mail.com";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired WorkoutSessionRepository sessionRepository;
    @Autowired WorkoutPlanRepository planRepository;
    @Autowired DataSeeder dataSeeder;

    User alice;
    Exercise bench;

    @BeforeEach
    void setUp() {
        dataSeeder.run(null);
        alice = userRepository.save(new User("Alice", ALICE, "hash"));
        userRepository.save(new User("Bob", BOB, "hash"));
        bench = exerciseRepository.findByNameIgnoreCaseAndUserIsNull("Bench Press").orElseThrow();
    }

    private void done(LocalDate date, String weight, int reps) {
        WorkoutSession s = new WorkoutSession(alice, null, "Past", UUID.randomUUID().toString(), date);
        SessionExercise e = new SessionExercise(bench, 1, 1, reps, null, null, 60);
        SessionSet set = new SessionSet(1);
        set.apply(reps, null, new BigDecimal(weight), true);
        e.addSet(set);
        s.addExercise(e);
        s.complete(Instant.now());
        sessionRepository.save(s);
    }

    private void setGoal(int goal) throws Exception {
        mockMvc.perform(put("/api/settings").with(user(ALICE)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"weightUnit\":\"KG\",\"weeklyGoal\":" + goal + ",\"theme\":\"LIGHT\",\"defaultRestSeconds\":60}"))
                .andExpect(status().isOk());
    }

    @Test
    void plannedWorkoutsAreNotCounted() throws Exception {
        WorkoutPlan plan = new WorkoutPlan(alice, "Planned", null, 1, false);
        plan.addExercise(new PlannedExercise(bench, 1, 3, 10, null, null, 60));
        planRepository.save(plan);
        mockMvc.perform(get("/api/progress/overview").with(user(ALICE)))
                .andExpect(jsonPath("$.totalCompleted").value(0))
                .andExpect(jsonPath("$.completedThisWeek").value(0))
                .andExpect(jsonPath("$.streakWeeks").value(0));
    }

    @Test
    void streakCountsConsecutiveWeeksThatMeetTheGoal() throws Exception {
        setGoal(1);
        done(LocalDate.now(), "40", 10);
        done(LocalDate.now().minusWeeks(1), "40", 10);
        done(LocalDate.now().minusWeeks(3), "40", 10); // gap at week -2 ends the streak
        mockMvc.perform(get("/api/progress/overview").with(user(ALICE)))
                .andExpect(jsonPath("$.streakWeeks").value(2))
                .andExpect(jsonPath("$.completedThisWeek").value(1))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.completionPercent").value(100));
    }

    @Test
    void unfinishedCurrentWeekDoesNotBreakTheStreak() throws Exception {
        setGoal(2);
        done(LocalDate.now(), "40", 10); // 1 of 2 this week
        done(LocalDate.now().minusWeeks(1), "40", 10);
        done(LocalDate.now().minusWeeks(1), "42", 10);
        mockMvc.perform(get("/api/progress/overview").with(user(ALICE)))
                .andExpect(jsonPath("$.streakWeeks").value(1))
                .andExpect(jsonPath("$.completionPercent").value(50));
    }

    @Test
    void exerciseProgressUsesTheHeaviestSetPerSession() throws Exception {
        done(LocalDate.now().minusDays(14), "40", 10);
        done(LocalDate.now().minusDays(7), "60", 5);
        done(LocalDate.now(), "50", 8);
        mockMvc.perform(get("/api/progress/exercises/" + bench.getId()).with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalBest.weightKg").value(60.0))
                .andExpect(jsonPath("$.latest.weightKg").value(50.0))
                .andExpect(jsonPath("$.previous.weightKg").value(60.0))
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].weightKg").value(40.0));
    }

    @Test
    void exerciseWithoutHistoryReturnsEmptyData() throws Exception {
        mockMvc.perform(get("/api/progress/exercises/" + bench.getId()).with(user(BOB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalBest").isEmpty())
                .andExpect(jsonPath("$.history.length()").value(0));
        mockMvc.perform(get("/api/progress/exercises/999999").with(user(BOB))).andExpect(status().isNotFound());
    }

    @Test
    void dashboardAndWeekAreCalculatedPerUser() throws Exception {
        done(LocalDate.now(), "40", 10);
        mockMvc.perform(get("/api/dashboard").with(user(ALICE)))
                .andExpect(jsonPath("$.totalCompleted").value(1))
                .andExpect(jsonPath("$.week.length()").value(7))
                .andExpect(jsonPath("$.recent.length()").value(1))
                .andExpect(jsonPath("$.strength[0].name").value("Bench Press"));
        mockMvc.perform(get("/api/dashboard").with(user(BOB)))
                .andExpect(jsonPath("$.totalCompleted").value(0))
                .andExpect(jsonPath("$.recent.length()").value(0))
                .andExpect(jsonPath("$.strength.length()").value(0));
        mockMvc.perform(get("/api/progress/week").with(user(ALICE)))
                .andExpect(jsonPath("$[0].dayName").value("Monday"))
                .andExpect(jsonPath("$[6].dayName").value("Sunday"));
    }
}