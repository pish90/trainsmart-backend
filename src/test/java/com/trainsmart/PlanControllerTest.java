package com.trainsmart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainsmart.dto.LogRequest;
import com.trainsmart.dto.LogResponse;
import com.trainsmart.dto.PlanRequest;
import com.trainsmart.dto.PlanResponse;
import com.trainsmart.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlanService planService;

    @Test
    void postPlans_returns200WithIdAndWeeks() throws Exception {
        PlanResponse response = new PlanResponse();
        response.setId("abc123def456ghij");
        response.setAthleteName("Test Athlete");
        response.setWeeks(List.of());
        response.setTotalWeeks(12);

        when(planService.createPlan(any(PlanRequest.class))).thenReturn(response);

        PlanRequest request = new PlanRequest();
        request.setAthleteName("Test Athlete");
        request.setRaceDate("2026-10-01");
        request.setRaceDistanceKm(106);
        request.setTargetTime("4:30");
        request.setFitnessLevel("intermediate");

        mockMvc.perform(post("/api/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123def456ghij"))
                .andExpect(jsonPath("$.totalWeeks").value(12));
    }

    @Test
    void getPlan_returns200WithProfileAndLogs() throws Exception {
        PlanResponse response = new PlanResponse();
        response.setId("test123");
        response.setAthleteName("Test Athlete");
        response.setRaceDate("2026-10-01");
        response.setLogs(Map.of());
        response.setWeeks(List.of());

        when(planService.getPlan("test123")).thenReturn(response);

        mockMvc.perform(get("/api/plans/test123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteName").value("Test Athlete"))
                .andExpect(jsonPath("$.raceDate").value("2026-10-01"));
    }

    @Test
    void getPlan_returns404ForUnknownId() throws Exception {
        when(planService.getPlan("unknown")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/plans/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSharedPlan_returns200ForValidCode() throws Exception {
        PlanResponse response = new PlanResponse();
        response.setId("test123");
        response.setAthleteName("Shared Athlete");
        response.setReadOnly(true);
        response.setWeeks(List.of());

        when(planService.getSharedPlan("SHARE123")).thenReturn(response);

        mockMvc.perform(get("/api/plans/share/SHARE123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readOnly").value(true));
    }

    @Test
    void getSharedPlan_returns404ForInvalidCode() throws Exception {
        when(planService.getSharedPlan("INVALID")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/plans/share/INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postLogs_returns200WithCoachNote() throws Exception {
        LogResponse logResponse = new LogResponse();
        logResponse.setSuccess(true);
        logResponse.setCoachNote("Great effort today!");

        when(planService.saveLog(eq("test123"), any(LogRequest.class))).thenReturn(logResponse);

        LogRequest request = new LogRequest();
        request.setSessionKey("w1_Tue");
        request.setStatus("done");
        request.setRpe(7);
        request.setNotes("Felt strong");
        request.setWeek(1);
        request.setDay("Tue");

        mockMvc.perform(post("/api/plans/test123/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coachNote").value("Great effort today!"))
                .andExpect(jsonPath("$.success").value(true));
    }
}
