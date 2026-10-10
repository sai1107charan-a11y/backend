package com.workouttracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.config.DataSeeder;
import com.workouttracker.dto.ProgramDto;
import com.workouttracker.repository.ProgramRepository;
import com.workouttracker.service.ProgramService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProgramApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramService programService;
    @Autowired DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder.run(null);
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/programs")).andExpect(status().isUnauthorized());
    }

    @Test
    void sixProgramsAreSeeded() throws Exception {
        mockMvc.perform(get("/api/programs").with(user("any@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].category").value("BODYBUILDING"));
    }

    @Test
    void seedingTwiceDoesNotCreateDuplicates() {
        dataSeeder.run(null);
        assertEquals(6, programRepository.count());
    }

    @Test
    void everyProgramCoversSevenDaysAndHasTrainingDays() {
        for (ProgramDto.Summary summary : programService.list()) {
            assertEquals(7, summary.trainingDays() + summary.restDays(), summary.name());
            assertFalse(summary.trainingDays() == 0, summary.name());
        }
    }

    @Test
    void detailListsSevenDaysMondayFirst() throws Exception {
        Long id = programRepository.findAllByOrderByIdAsc().get(0).getId();
        mockMvc.perform(get("/api/programs/" + id).with(user("any@mail.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].dayName").value("Monday"))
                .andExpect(jsonPath("$.days[0].exercises[0].exerciseName").value("Bench Press"));
    }

    @Test
    void unknownProgramReturns404() throws Exception {
        mockMvc.perform(get("/api/programs/999999").with(user("any@mail.com")))
                .andExpect(status().isNotFound());
    }
}