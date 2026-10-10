package com.workouttracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

    private static final String REGISTER =
            "{\"name\":\"Ali\",\"email\":\"Ali@Mail.com\",\"password\":\"secret123\"}";

    @Autowired MockMvc mockMvc;

    private void register() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register();
        mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER.replace("Ali@Mail.com", "ALI@mail.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register();
        mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ali@mail.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginCreatesSessionThatMeEndpointAccepts() throws Exception {
        register();

        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ali@mail.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ali@mail.com"))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ali"));
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }
}