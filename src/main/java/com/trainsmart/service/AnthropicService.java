package com.trainsmart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trainsmart.dto.PlanRequest;
import com.trainsmart.entity.TrainingPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    @Value("${anthropic.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public JsonNode generatePlan(PlanRequest profile) {
        int weeks = calculateWeeks(profile.getRaceDate());
        String prompt = buildGeneratePlanPrompt(profile, weeks);
        String response = callAnthropic(prompt, 7000);
        return extractAndParseJson(response);
    }

    public Map<String, Object> reviewAndUpdate(TrainingPlan plan, String newEntryKey, String newEntryJson) {
        String prompt = buildReviewPrompt(plan, newEntryKey, newEntryJson);
        String response = callAnthropic(prompt, 5000);
        return parseReviewResponse(response);
    }

    public double parseHours(String targetTime) {
        if (targetTime == null || targetTime.isBlank()) return 0;
        String[] parts = targetTime.split(":");
        if (parts.length < 2) {
            try { return Double.parseDouble(targetTime); } catch (NumberFormatException e) { return 0; }
        }
        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]) / 60.0;
        return hours + minutes;
    }

    private String buildGeneratePlanPrompt(PlanRequest profile, int weeks) {
        return String.format("""
                You are an expert cycling coach. Generate a structured %d-week training plan for the following athlete.

                Athlete Profile:
                - Name: %s
                - Race Date: %s
                - Race Distance: %d km
                - Target Time: %s
                - Fitness Level: %s

                Requirements:
                - Exactly 6 sessions per week, always on these days:
                  * Tuesday: Cycling (easy, interval, or race zone)
                  * Wednesday: Strength training
                  * Thursday: Cycling (easy, interval, or race zone)
                  * Friday: Rest
                  * Saturday: Long ride (long zone)
                  * Sunday: Mobility/recovery
                - Zones allowed: easy, interval, race, long, strength, mobility, rest
                - Each session must have: day, type, label, duration (e.g. "1h 30m"), detail, zone
                - Progressive overload: build volume weeks 1-%d, taper last 2 weeks

                Return ONLY valid JSON in this exact structure, no markdown, no explanation:
                {
                  "weeks": [
                    {
                      "week": 1,
                      "theme": "Base Building",
                      "sessions": [
                        {"day": "Tue", "type": "Cycling", "label": "Easy spin", "duration": "1h", "detail": "Zone 2 effort, keep HR below 140", "zone": "easy"},
                        {"day": "Wed", "type": "Strength", "label": "Leg strength", "duration": "45m", "detail": "Squats, lunges, core work", "zone": "strength"},
                        {"day": "Thu", "type": "Cycling", "label": "Tempo intervals", "duration": "1h 15m", "detail": "3x10min at threshold", "zone": "interval"},
                        {"day": "Fri", "type": "Rest", "label": "Rest day", "duration": "0m", "detail": "Full recovery", "zone": "rest"},
                        {"day": "Sat", "type": "Cycling", "label": "Long ride", "duration": "3h", "detail": "Steady endurance pace", "zone": "long"},
                        {"day": "Sun", "type": "Mobility", "label": "Recovery yoga", "duration": "30m", "detail": "Hip flexors, hamstrings, lower back", "zone": "mobility"}
                      ]
                    }
                  ]
                }
                """,
                weeks,
                profile.getAthleteName(),
                profile.getRaceDate(),
                profile.getRaceDistanceKm(),
                profile.getTargetTime(),
                profile.getFitnessLevel(),
                Math.max(weeks - 2, 1));
    }

    private String buildReviewPrompt(TrainingPlan plan, String newEntryKey, String newEntryJson) {
        return String.format("""
                You are a cycling coach reviewing an athlete's training progress.

                Athlete: %s
                Race: %d km on %s, target %s
                Fitness level: %s

                Current logs: %s
                New session logged (%s): %s

                Based on this new log entry, decide if the plan needs adjustment.
                Return ONLY valid JSON:
                {
                  "shouldUpdate": false,
                  "reasoning": "Brief explanation",
                  "coachNote": "Personal encouraging message to athlete (2-3 sentences)",
                  "updatedWeeks": []
                }

                Only set shouldUpdate to true if there's a clear need (injury risk, repeated misses, significantly over/under performing).
                coachNote must always be present and feel personal and motivating.
                """,
                plan.getAthleteName(),
                plan.getRaceDistanceKm(),
                plan.getRaceDate(),
                plan.getTargetTime(),
                plan.getFitnessLevel(),
                plan.getLogsJson() != null ? plan.getLogsJson() : "{}",
                newEntryKey,
                newEntryJson);
    }

    String callAnthropic(String prompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);

        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request", e);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Anthropic API error: " + response.getStatusCode());
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            return responseNode.at("/content/0/text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    private JsonNode extractAndParseJson(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\n?", "").replaceAll("```", "").trim();
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse Anthropic JSON response: {}", text, e);
            throw new RuntimeException("Failed to parse plan JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseReviewResponse(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\n?", "").replaceAll("```", "").trim();
            }
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse review response: {}", text, e);
            return Map.of(
                    "shouldUpdate", false,
                    "reasoning", "Unable to parse response",
                    "coachNote", "Great work logging your session! Keep it up.",
                    "updatedWeeks", java.util.List.of()
            );
        }
    }

    private int calculateWeeks(String raceDate) {
        try {
            java.time.LocalDate race = java.time.LocalDate.parse(raceDate);
            java.time.LocalDate today = java.time.LocalDate.now();
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, race);
            return (int) Math.max(4, Math.min(24, days / 7));
        } catch (Exception e) {
            return 12;
        }
    }
}
