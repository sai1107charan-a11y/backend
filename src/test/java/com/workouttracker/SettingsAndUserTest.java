package com.workouttracker;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.workouttracker.model.User;
import com.workouttracker.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingsAndUserTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.save(new User("Alice", "alice@mail.com", passwordEncoder.encode("secret123")));
        userRepository.save(new User("Bob", "bob@mail.com", passwordEncoder.encode("secret123")));
    }

    private String settings(String unit, int goal, String theme, int rest) {
        return "{\"weightUnit\":\"" + unit + "\",\"weeklyGoal\":" + goal + ",\"theme\":\"" + theme
                + "\",\"defaultRestSeconds\":" + rest + "}";
    }

    @Test
    void defaultsAreReturnedBeforeAnythingIsSaved() throws Exception {
        mockMvc.perform(get("/api/settings").with(user("alice@mail.com")))
                .andExpect(jsonPath("$.weightUnit").value("KG"))
                .andExpect(jsonPath("$.weeklyGoal").value(3))
                .andExpect(jsonPath("$.theme").value("LIGHT"))
                .andExpect(jsonPath("$.defaultRestSeconds").value(60));
    }

    @Test
    void settingsArePersistedPerUser() throws Exception {
        mockMvc.perform(put("/api/settings").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(settings("LB", 5, "DARK", 90)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/settings").with(user("alice@mail.com")))
                .andExpect(jsonPath("$.weightUnit").value("LB")).andExpect(jsonPath("$.weeklyGoal").value(5));
        mockMvc.perform(get("/api/settings").with(user("bob@mail.com")))
                .andExpect(jsonPath("$.weightUnit").value("KG"));
    }

    @Test
    void invalidSettingsAreRejected() throws Exception {
        for (String body : new String[]{settings("KG", 0, "LIGHT", 60), settings("KG", 3, "LIGHT", 2),
                settings("STONE", 3, "LIGHT", 60), settings("KG", 3, "NEON", 60)}) {
            mockMvc.perform(put("/api/settings").with(user("alice@mail.com")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void renameAndPasswordChange() throws Exception {
        mockMvc.perform(put("/api/users/me").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Alice B\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alice B"))
                .andExpect(content().string(not(containsString("password"))));

        mockMvc.perform(put("/api/users/me/password").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newsecret1\"}"))
                .andExpect(status().isBadRequest());           // 400, not 401

        mockMvc.perform(put("/api/users/me/password").with(user("alice@mail.com")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"secret123\",\"newPassword\":\"newsecret1\"}"))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("newsecret1",
                userRepository.findByEmail("alice@mail.com").orElseThrow().getPassword()));
    }
}