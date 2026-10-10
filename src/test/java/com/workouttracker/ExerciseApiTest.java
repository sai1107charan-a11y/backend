package com.workouttracker;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.config.DataSeeder;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.model.User;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExerciseApiTest {

    private static final String BODY =
            "{\"name\":\"Cable Woodchop\",\"muscleGroup\":\"CORE\",\"equipment\":\"Cable machine\",\"description\":\"Rotate the torso.\"}";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired DataSeeder dataSeeder;

    Exercise aliceCustom;

    @BeforeEach
    void setUp() {
        dataSeeder.run(null); // safe: skips anything that already exists
        User alice = userRepository.save(new User("Alice", "alice@mail.com", "hash"));
        userRepository.save(new User("Bob", "bob@mail.com", "hash"));

        Exercise custom = new Exercise("Alice Special", MuscleGroup.CORE, "None", null);
        custom.setUser(alice);
        aliceCustom = exerciseRepository.save(custom);
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    void groupFilterReturnsOnlyThatGroup() throws Exception {
        mockMvc.perform(get("/api/exercises?group=CHEST").with(user("alice@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[*].muscleGroup", everyItem(is("CHEST"))));
    }

    @Test
    void searchFindsByName() throws Exception {
        mockMvc.perform(get("/api/exercises?search=bench").with(user("alice@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Bench Press")));
    }

    @Test
    void invalidGroupIsRejected() throws Exception {
        mockMvc.perform(get("/api/exercises?group=ARMS").with(user("alice@mail.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customExerciseIsPrivateToItsOwner() throws Exception {
        mockMvc.perform(get("/api/exercises?search=alice special").with(user("alice@mail.com")))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/exercises?search=alice special").with(user("bob@mail.com")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createReturns201AndMarksExerciseAsCustom() throws Exception {
        mockMvc.perform(post("/api/exercises").with(user("bob@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.muscleGroupLabel").value("Abs and Core"));
    }

    @Test
    void duplicateNameIsRejectedEvenForBuiltInNames() throws Exception {
        mockMvc.perform(post("/api/exercises").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("Cable Woodchop", "bench press")))
                .andExpect(status().isConflict());
    }

    @Test
    void validationErrorsReturn400() throws Exception {
        mockMvc.perform(post("/api/exercises").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"muscleGroup\":\"CORE\",\"equipment\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/exercises").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"muscleGroup\":\"ARMS\",\"equipment\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUserCannotEditOrDeleteMyCustomExercise() throws Exception {
        String url = "/api/exercises/" + aliceCustom.getId();
        mockMvc.perform(put(url).with(user("bob@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
        mockMvc.perform(delete(url).with(user("bob@mail.com")).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void ownerCanEditAndDelete() throws Exception {
        String url = "/api/exercises/" + aliceCustom.getId();
        mockMvc.perform(put(url).with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cable Woodchop"));
        mockMvc.perform(delete(url).with(user("alice@mail.com")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void builtInExercisesCannotBeChanged() throws Exception {
        Exercise builtIn = exerciseRepository.findByNameIgnoreCaseAndUserIsNull("Bench Press").orElseThrow();
        String url = "/api/exercises/" + builtIn.getId();
        mockMvc.perform(put(url).with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mockMvc.perform(delete(url).with(user("alice@mail.com")).with(csrf())).andExpect(status().isForbidden());
    }
}