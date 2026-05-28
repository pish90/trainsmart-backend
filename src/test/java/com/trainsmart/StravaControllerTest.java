package com.trainsmart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StravaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void webhookVerification_returnsHubChallenge() throws Exception {
        mockMvc.perform(get("/api/strava/webhook")
                        .param("hub.mode", "subscribe")
                        .param("hub.challenge", "testchallenge123")
                        .param("hub.verify_token", "test-verify-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['hub.challenge']").value("testchallenge123"));
    }

    @Test
    void healthCheck_returnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
