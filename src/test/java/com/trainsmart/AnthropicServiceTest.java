package com.trainsmart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainsmart.dto.PlanRequest;
import com.trainsmart.service.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnthropicServiceTest {

    private AnthropicService service;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        service = new AnthropicService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.anthropic.com/v1/messages");
        ReflectionTestUtils.setField(service, "model", "claude-sonnet-4-20250514");
    }

    @Test
    void generatePlan_constructsCorrectPromptStructure() {
        String mockResponse = """
                {
                  "id": "msg_123",
                  "content": [{"type": "text", "text": "{\\"weeks\\": [{\\\"week\\\": 1, \\\"sessions\\\": []}]}"}],
                  "model": "claude-sonnet-4-20250514"
                }
                """;
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        PlanRequest profile = new PlanRequest();
        profile.setAthleteName("Test Athlete");
        profile.setRaceDate("2026-10-01");
        profile.setRaceDistanceKm(106);
        profile.setTargetTime("4:30");
        profile.setFitnessLevel("intermediate");

        JsonNode result = service.generatePlan(profile);

        assertThat(result).isNotNull();
        assertThat(result.has("weeks")).isTrue();

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
        String body = captor.getValue().getBody().toString();
        assertThat(body).contains("claude-sonnet-4-20250514");
        assertThat(body).contains("max_tokens");
    }

    @Test
    void reviewAndUpdate_returnsCorrectFields() {
        String mockResponse = """
                {
                  "content": [{"type": "text", "text": "{\\"shouldUpdate\\": false, \\"reasoning\\": \\"Good session\\", \\"coachNote\\": \\"Great work!\\", \\"updatedWeeks\\": []}"}],
                  "model": "claude-sonnet-4-20250514"
                }
                """;
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        com.trainsmart.entity.TrainingPlan plan = new com.trainsmart.entity.TrainingPlan();
        plan.setId("test123");
        plan.setAthleteName("Test");
        plan.setRaceDate("2026-10-01");
        plan.setRaceDistanceKm(106);
        plan.setTargetTime("4:30");
        plan.setFitnessLevel("intermediate");
        plan.setLogsJson("{}");

        java.util.Map<String, Object> result = service.reviewAndUpdate(plan, "w1_Tue", "{\"status\":\"done\"}");

        assertThat(result).containsKey("shouldUpdate");
        assertThat(result).containsKey("coachNote");
        assertThat(result.get("coachNote")).isEqualTo("Great work!");
    }

    @Test
    void parseHours_fourThirty_returnsFourPointFive() {
        assertThat(service.parseHours("4:30")).isEqualTo(4.5);
    }

    @Test
    void parseHours_fiveZero_returnsFive() {
        assertThat(service.parseHours("5:00")).isEqualTo(5.0);
    }
}
