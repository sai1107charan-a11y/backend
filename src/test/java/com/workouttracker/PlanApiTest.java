package com.workouttracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.workouttracker.config.DataSeeder;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.model.User;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.ProgramRepository;
import com.workouttracker.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanApiTest {

    private static final String ALICE = "alice@mail.com";
    private static final String BOB = "bob@mail.com";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ProgramRepository programRepository;
    @Autowired DataSeeder dataSeeder;

    Long benchId;
    Long squatId;
    User alice;

    @BeforeEach
    void setUp() {
        dataSeeder.run(null);
        alice = userRepository.save(new User("Alice", ALICE, "hash"));
        userRepository.save(new User("Bob", BOB, "hash"));
        benchId = exerciseRepository.findByNameIgnoreCaseAndUserIsNull("Bench Press").orElseThrow().getId();
        squatId = exerciseRepository.findByNameIgnoreCaseAndUserIsNull("Squat").orElseThrow().getId();
    }

    // day is the text "1".."7" or "null"
    private String workoutJson(String name, String day, Long exerciseId) {
        return "{\"name\":\"" + name + "\",\"category\":\"CHEST\",\"dayOfWeek\":" + day
                + ",\"restDay\":false,\"exercises\":[{\"exerciseId\":" + exerciseId
                + ",\"sets\":3,\"reps\":10,\"targetWeight\":40,\"restSeconds\":90}]}";
    }

    private long createPlan(String email, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/plans").with(user(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void expectCreateStatus(String email, String url, String json, int expected) throws Exception {
        mockMvc.perform(post(url).with(user(email)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expected));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void createReturns201WithExercises() throws Exception {
        mockMvc.perform(post("/api/plans").with(user(ALICE)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(workoutJson("Push Day", "1", benchId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayName").value("Monday"))
                .andExpect(jsonPath("$.categoryLabel").value("Chest"))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Bench Press"))
                .andExpect(jsonPath("$.exercises[0].targetWeight").value(40.0));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        // no exercises
        expectCreateStatus(ALICE, "/api/plans",
                "{\"name\":\"Empty\",\"dayOfWeek\":null,\"restDay\":false,\"exercises\":[]}", 400);
        // missing name
        expectCreateStatus(ALICE, "/api/plans", workoutJson("", "null", benchId), 400);
        // sets = 0
        expectCreateStatus(ALICE, "/api/plans", workoutJson("X", "null", benchId).replace("\"sets\":3", "\"sets\":0"), 400);
        // negative weight
        expectCreateStatus(ALICE, "/api/plans",
                workoutJson("X", "null", benchId).replace("\"targetWeight\":40", "\"targetWeight\":-5"), 400);
        // both reps and duration
        expectCreateStatus(ALICE, "/api/plans",
                workoutJson("X", "null", benchId).replace("\"reps\":10", "\"reps\":10,\"durationSeconds\":30"), 400);
        // neither reps nor duration
        expectCreateStatus(ALICE, "/api/plans",
                workoutJson("X", "null", benchId).replace("\"reps\":10,", ""), 400);
        // day 9
        expectCreateStatus(ALICE, "/api/plans", workoutJson("X", "9", benchId), 400);
        // unknown exercise
        expectCreateStatus(ALICE, "/api/plans", workoutJson("X", "null", 999999L), 400);
    }

    @Test
    void takenDayReturns409UntilReplaceIsTrue() throws Exception {
        createPlan(ALICE, workoutJson("Chest", "1", benchId));

        mockMvc.perform(post("/api/plans").with(user(ALICE)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(workoutJson("Other", "1", squatId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That day already has a planned workout"));

        expectCreateStatus(ALICE, "/api/plans?replace=true", workoutJson("Other", "1", squatId), 201);

        mockMvc.perform(get("/api/plans").with(user(ALICE)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Other"));
    }

    @Test
    void restDayRules() throws Exception {
        expectCreateStatus(ALICE, "/api/plans",
                "{\"name\":\"Rest Day\",\"dayOfWeek\":7,\"restDay\":true,\"exercises\":[]}", 201);
        // rest day without a day
        expectCreateStatus(ALICE, "/api/plans",
                "{\"name\":\"Rest Day\",\"dayOfWeek\":null,\"restDay\":true,\"exercises\":[]}", 400);
        // rest day with exercises
        expectCreateStatus(ALICE, "/api/plans",
                workoutJson("X", "6", benchId).replace("\"restDay\":false", "\"restDay\":true"), 400);
    }

    @Test
    void otherUserCannotSeeChangeOrCopyMyPlan() throws Exception {
        long id = createPlan(ALICE, workoutJson("Private", "2", benchId));
        String url = "/api/plans/" + id;

        mockMvc.perform(get("/api/plans").with(user(BOB))).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(url).with(user(BOB))).andExpect(status().isNotFound());
        mockMvc.perform(put(url).with(user(BOB)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(workoutJson("Hack", "2", benchId))).andExpect(status().isNotFound());
        mockMvc.perform(delete(url).with(user(BOB)).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post(url + "/duplicate").with(user(BOB)).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post(url + "/copy?day=3").with(user(BOB)).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(put(url + "/day?day=4").with(user(BOB)).with(csrf())).andExpect(status().isNotFound());

        mockMvc.perform(get(url).with(user(ALICE))).andExpect(status().isOk());
    }

    @Test
    void ownerCanUpdateAndDelete() throws Exception {
        long id = createPlan(ALICE, workoutJson("Old", "2", benchId));
        mockMvc.perform(put("/api/plans/" + id).with(user(ALICE)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(workoutJson("New", "2", squatId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.exercises.length()").value(1))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("Squat"));
        mockMvc.perform(delete("/api/plans/" + id).with(user(ALICE)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/plans/" + id).with(user(ALICE))).andExpect(status().isNotFound());
    }

    @Test
    void duplicateCreatesAnUnscheduledCopy() throws Exception {
        long id = createPlan(ALICE, workoutJson("Leg Day", "3", squatId));
        mockMvc.perform(post("/api/plans/" + id + "/duplicate").with(user(ALICE)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Leg Day (copy)"))
                .andExpect(jsonPath("$.dayOfWeek").isEmpty())
                .andExpect(jsonPath("$.exercises.length()").value(1));
    }

    @Test
    void copyToDayKeepsTheOriginal() throws Exception {
        long id = createPlan(ALICE, workoutJson("Chest", "1", benchId));
        mockMvc.perform(post("/api/plans/" + id + "/copy?day=3").with(user(ALICE)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayName").value("Wednesday"));
        mockMvc.perform(get("/api/plans").with(user(ALICE))).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(post("/api/plans/" + id + "/copy?day=1").with(user(ALICE)).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moveToDayAndBackOffTheWeek() throws Exception {
        long id = createPlan(ALICE, workoutJson("Floating", "null", benchId));
        mockMvc.perform(put("/api/plans/" + id + "/day?day=2").with(user(ALICE)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayName").value("Tuesday"));
        mockMvc.perform(put("/api/plans/" + id + "/day").with(user(ALICE)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").isEmpty());
    }

    @Test
    void usingAProgramCopiesItAndLeavesTheTemplateUnchanged() throws Exception {
        Long programId = programRepository.findAllByOrderByIdAsc().get(0).getId();

        mockMvc.perform(post("/api/plans/from-program/" + programId).with(user(BOB)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(7));

        MvcResult list = mockMvc.perform(get("/api/plans").with(user(BOB)))
                .andExpect(jsonPath("$[0].dayName").value("Monday"))
                .andExpect(jsonPath("$[0].sourceProgram").value("Bodybuilding Muscle Split"))
                .andExpect(jsonPath("$[0].exercises.length()").value(5))
                .andReturn();
        Number mondayId = JsonPath.read(list.getResponse().getContentAsString(), "$[0].id");

        // Edit Bob's Monday down to one exercise
        mockMvc.perform(put("/api/plans/" + mondayId.longValue()).with(user(BOB)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(workoutJson("Mine", "1", benchId)))
                .andExpect(status().isOk());

        // The template still has its 5 Monday exercises
        mockMvc.perform(get("/api/programs/" + programId).with(user(BOB)))
                .andExpect(jsonPath("$.days[0].exercises.length()").value(5));
        // And Alice has no plans at all
        mockMvc.perform(get("/api/plans").with(user(ALICE))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void usingAProgramTwiceNeedsReplace() throws Exception {
        Long programId = programRepository.findAllByOrderByIdAsc().get(0).getId();
        expectCreateStatus(ALICE, "/api/plans/from-program/" + programId, "", 201);
        expectCreateStatus(ALICE, "/api/plans/from-program/" + programId, "", 409);
        expectCreateStatus(ALICE, "/api/plans/from-program/" + programId + "?replace=true", "", 201);
        mockMvc.perform(get("/api/plans").with(user(ALICE))).andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void sampleScheduleHasSevenDaysEndingWithRest() throws Exception {
        mockMvc.perform(post("/api/plans/sample-schedule").with(user(ALICE)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].name").value("Chest and Triceps"))
                .andExpect(jsonPath("$[6].restDay").value(true));
        expectCreateStatus(ALICE, "/api/plans/sample-schedule", "", 409);
    }

    @Test
    void anExerciseUsedByAPlanCannotBeDeleted() throws Exception {
        Exercise custom = new Exercise("My Move", MuscleGroup.CORE, "None", null);
        custom.setUser(alice);
        custom = exerciseRepository.save(custom);

        createPlan(ALICE, workoutJson("Uses custom", "null", custom.getId()));
        mockMvc.perform(delete("/api/exercises/" + custom.getId()).with(user(ALICE)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherUsersCustomExerciseCannotBeUsed() throws Exception {
        Exercise custom = new Exercise("Alice Only", MuscleGroup.CORE, "None", null);
        custom.setUser(alice);
        custom = exerciseRepository.save(custom);

        expectCreateStatus(BOB, "/api/plans", workoutJson("Sneaky", "null", custom.getId()), 400);
    }
}