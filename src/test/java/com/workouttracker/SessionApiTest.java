package com.workouttracker;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.workouttracker.config.DataSeeder;
import com.workouttracker.model.Exercise;
import com.workouttracker.model.MuscleGroup;
import com.workouttracker.model.PlannedExercise;
import com.workouttracker.model.User;
import com.workouttracker.model.WorkoutPlan;
import com.workouttracker.repository.ExerciseRepository;
import com.workouttracker.repository.UserRepository;
import com.workouttracker.repository.WorkoutPlanRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionApiTest {

    static final String ALICE = "alice@mail.com";
    static final String BOB = "bob@mail.com";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired WorkoutPlanRepository planRepository;
    @Autowired DataSeeder dataSeeder;

    Long planId;

    @BeforeEach
    void setUp() {
        dataSeeder.run(null);
        User alice = userRepository.save(new User("Alice", ALICE, "hash"));
        userRepository.save(new User("Bob", BOB, "hash"));
        Exercise bench = exerciseRepository.findByNameIgnoreCaseAndUserIsNull("Bench Press").orElseThrow();
        WorkoutPlan plan = new WorkoutPlan(alice, "Push Day", MuscleGroup.CHEST, 1, false);
        plan.addExercise(new PlannedExercise(bench, 1, 2, 10, null, new BigDecimal("40"), 60));
        planId = planRepository.save(plan).getId();
    }

    private MvcResult start(String email, String token) throws Exception {
        return mockMvc.perform(post("/api/workout-sessions").with(user(email)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":" + planId + ",\"clientToken\":\"" + token + "\"}")).andReturn();
    }

    private long startOk(String token) throws Exception {
        MvcResult result = start(ALICE, token);
        org.junit.jupiter.api.Assertions.assertEquals(201, result.getResponse().getStatus());
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private long firstSetId(long sessionId) throws Exception {
        String json = mockMvc.perform(get("/api/workout-sessions/" + sessionId).with(user(ALICE)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.exercises[0].sets[0].id")).longValue();
    }

    private void completeSet(String email, long sessionId, long setId, String weight, int expected) throws Exception {
        mockMvc.perform(put("/api/workout-sessions/" + sessionId + "/sets/" + setId).with(user(email)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actualReps\":10,\"actualWeight\":" + weight + ",\"completed\":true}"))
                .andExpect(status().is(expected));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/workout-sessions/active")).andExpect(status().isUnauthorized());
    }

    @Test
    void startCopiesThePlanIntoSets() throws Exception {
        MvcResult result = start(ALICE, "t1");
        org.junit.jupiter.api.Assertions.assertEquals(201, result.getResponse().getStatus());
        String json = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS", JsonPath.read(json, "$.status"));
        org.junit.jupiter.api.Assertions.assertEquals(2, (Integer) JsonPath.read(json, "$.totalSets"));
        org.junit.jupiter.api.Assertions.assertEquals("Bench Press", JsonPath.read(json, "$.exercises[0].exerciseName"));
    }

    @Test
    void sameTokenReturnsSameSessionAndSecondActiveIsRejected() throws Exception {
        long first = startOk("t1");
        long again = startOk("t1");
        org.junit.jupiter.api.Assertions.assertEquals(first, again);
        org.junit.jupiter.api.Assertions.assertEquals(409, start(ALICE, "t2").getResponse().getStatus());
    }

    @Test
    void otherUserCannotTouchMySession() throws Exception {
        long id = startOk("t1");
        long setId = firstSetId(id);
        mockMvc.perform(get("/api/workout-sessions/" + id).with(user(BOB))).andExpect(status().isNotFound());
        completeSet(BOB, id, setId, "40", 404);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(BOB)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/workout-sessions/" + id + "/abandon").with(user(BOB)).with(csrf()))
                .andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertEquals(404, start(BOB, "bobtoken").getResponse().getStatus()); // Alice's plan
    }

    @Test
    void setValidation() throws Exception {
        long id = startOk("t1");
        long setId = firstSetId(id);
        String url = "/api/workout-sessions/" + id + "/sets/" + setId;
        for (String body : new String[]{
                "{\"actualReps\":0,\"completed\":true}",
                "{\"actualReps\":10,\"actualWeight\":-1,\"completed\":true}",
                "{\"completed\":true}"}) {
            mockMvc.perform(put(url).with(user(ALICE)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void finishNeedsACompletedSetAndLocksTheSession() throws Exception {
        long id = startOk("t1");
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(ALICE)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at least one set")));

        long setId = firstSetId(id);
        completeSet(ALICE, id, setId, "42.5", 200);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(ALICE)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedSets").value(1));
        completeSet(ALICE, id, setId, "50", 409);
        mockMvc.perform(get("/api/workout-sessions/active").with(user(ALICE))).andExpect(status().isNoContent());
    }

    @Test
    void abandonedSessionsAreNotHistory() throws Exception {
        long id = startOk("t1");
        completeSet(ALICE, id, firstSetId(id), "40", 200);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/abandon").with(user(ALICE)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/workout-sessions").with(user(ALICE))).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/progress/overview").with(user(ALICE))).andExpect(jsonPath("$.totalCompleted").value(0));
    }

    @Test
    void historyFiltersWork() throws Exception {
        long id = startOk("t1");
        completeSet(ALICE, id, firstSetId(id), "40", 200);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(ALICE)).with(csrf()));

        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/workout-sessions").with(user(ALICE))).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/workout-sessions?search=bench").with(user(ALICE))).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/workout-sessions?search=squat").with(user(ALICE))).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/workout-sessions?from=" + today + "&to=" + today).with(user(ALICE)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/workout-sessions?from=" + tomorrow).with(user(ALICE)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/workout-sessions").with(user(BOB))).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/workout-sessions/dates?month=" + today.substring(0, 7)).with(user(ALICE)))
                .andExpect(jsonPath("$[0].count").value(1));
        mockMvc.perform(get("/api/workout-sessions/dates?month=bad").with(user(ALICE))).andExpect(status().isBadRequest());
    }

    @Test
    void reuseAsRoutineUsesTheWeightActuallyLifted() throws Exception {
        long id = startOk("t1");
        completeSet(ALICE, id, firstSetId(id), "47.5", 200);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(ALICE)).with(csrf()));
        mockMvc.perform(post("/api/workout-sessions/" + id + "/to-routine").with(user(ALICE)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push Day (from history)"))
                .andExpect(jsonPath("$.dayOfWeek").isEmpty())
                .andExpect(jsonPath("$.exercises[0].targetWeight").value(47.5));
    }

    @Test
    void csvExportContainsCompletedSetsOnly() throws Exception {
        long id = startOk("t1");
        completeSet(ALICE, id, firstSetId(id), "40", 200);
        mockMvc.perform(post("/api/workout-sessions/" + id + "/finish").with(user(ALICE)).with(csrf()));
        mockMvc.perform(get("/api/workout-sessions/export").with(user(ALICE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"Bench Press\",1,10,,40.00")));
        mockMvc.perform(get("/api/workout-sessions/export").with(user(BOB)))
                .andExpect(content().string("date,workout,exercise,set,reps,seconds,weight_kg\n"));
    }
}