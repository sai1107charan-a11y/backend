package com.workouttracker;

import com.workouttracker.model.User;
import com.workouttracker.model.Workout;
import com.workouttracker.repository.UserRepository;
import com.workouttracker.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkoutOwnershipTest {

    private static final String BODY =
            "{\"exerciseName\":\"Squat\",\"sets\":4,\"reps\":8,\"weight\":60,\"workoutDate\":\"2026-10-04\"}";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WorkoutRepository workoutRepository;

    Workout aliceWorkout;

    @BeforeEach
    void setUp() {
        User alice = userRepository.save(new User("Alice", "alice@mail.com", "hash"));
        userRepository.save(new User("Bob", "bob@mail.com", "hash"));

        Workout workout = new Workout("Bench Press", 3, 10, 40.0, LocalDate.of(2026, 10, 3));
        workout.setUser(alice);
        aliceWorkout = workoutRepository.save(workout);
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/workouts")).andExpect(status().isUnauthorized());
    }

    @Test
    void userSeesOnlyOwnWorkouts() throws Exception {
        mockMvc.perform(get("/api/workouts").with(user("alice@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/workouts").with(user("bob@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void otherUserCannotReadUpdateOrDelete() throws Exception {
        String url = "/api/workouts/" + aliceWorkout.getId();

        mockMvc.perform(get(url).with(user("bob@mail.com"))).andExpect(status().isNotFound());
        mockMvc.perform(put(url).with(user("bob@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
        mockMvc.perform(delete(url).with(user("bob@mail.com")).with(csrf()))
                .andExpect(status().isNotFound());

        assertTrue(workoutRepository.existsById(aliceWorkout.getId()));
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        String url = "/api/workouts/" + aliceWorkout.getId();

        mockMvc.perform(put(url).with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName").value("Squat"));
        mockMvc.perform(delete(url).with(user("alice@mail.com")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void createdWorkoutBelongsOnlyToCreator() throws Exception {
        mockMvc.perform(post("/api/workouts").with(user("bob@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/workouts").with(user("bob@mail.com")))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/workouts").with(user("alice@mail.com")))
                .andExpect(jsonPath("$.length()").value(1)); // still only her own
    }

    @Test
    void writeWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/workouts").with(user("alice@mail.com"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}