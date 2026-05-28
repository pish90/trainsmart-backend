package com.trainsmart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trainsmart.dto.LogRequest;
import com.trainsmart.dto.LogResponse;
import com.trainsmart.dto.PlanRequest;
import com.trainsmart.dto.PlanResponse;
import com.trainsmart.entity.TrainingPlan;
import com.trainsmart.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final TrainingPlanRepository planRepository;
    private final AnthropicService anthropicService;
    private final ObjectMapper objectMapper;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom random = new SecureRandom();

    public PlanResponse createPlan(PlanRequest request) {
        JsonNode planData = anthropicService.generatePlan(request);

        TrainingPlan plan = new TrainingPlan();
        plan.setId(generateId(16));
        plan.setAthleteName(request.getAthleteName());
        plan.setRaceDate(request.getRaceDate());
        plan.setRaceDistanceKm(request.getRaceDistanceKm());
        plan.setTargetTime(request.getTargetTime());
        plan.setFitnessLevel(request.getFitnessLevel());

        JsonNode weeksNode = planData.get("weeks");
        plan.setTotalWeeks(weeksNode != null ? weeksNode.size() : 0);
        plan.setPlanJson(planData.toString());
        plan.setLogsJson("{}");

        planRepository.save(plan);
        return toPlanResponse(plan, false);
    }

    public PlanResponse getPlan(String id) {
        TrainingPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + id));
        return toPlanResponse(plan, false);
    }

    public PlanResponse getSharedPlan(String shareCode) {
        TrainingPlan plan = planRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new RuntimeException("Share code not found: " + shareCode));
        return toPlanResponse(plan, true);
    }

    public LogResponse saveLog(String planId, LogRequest request) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        try {
            ObjectNode logs = plan.getLogsJson() != null
                    ? (ObjectNode) objectMapper.readTree(plan.getLogsJson())
                    : objectMapper.createObjectNode();

            String logEntryJson = objectMapper.writeValueAsString(Map.of(
                    "status", request.getStatus(),
                    "rpe", request.getRpe() != null ? request.getRpe() : 0,
                    "notes", request.getNotes() != null ? request.getNotes() : "",
                    "loggedAt", java.time.Instant.now().toString()
            ));

            logs.put(request.getSessionKey(), objectMapper.readTree(logEntryJson));
            plan.setLogsJson(logs.toString());
            planRepository.save(plan);

            Map<String, Object> review = anthropicService.reviewAndUpdate(plan, request.getSessionKey(), logEntryJson);

            if (Boolean.TRUE.equals(review.get("shouldUpdate")) && review.get("updatedWeeks") instanceof List<?> updatedWeeks && !updatedWeeks.isEmpty()) {
                try {
                    JsonNode planNode = objectMapper.readTree(plan.getPlanJson());
                    ObjectNode planObj = (ObjectNode) planNode;
                    planObj.set("weeks", objectMapper.valueToTree(updatedWeeks));
                    plan.setPlanJson(planObj.toString());
                    planRepository.save(plan);
                } catch (Exception e) {
                    log.error("Failed to update plan weeks", e);
                }
            }

            LogResponse response = new LogResponse();
            response.setSuccess(true);
            response.setCoachNote((String) review.get("coachNote"));
            response.setPlanUpdated(Boolean.TRUE.equals(review.get("shouldUpdate")));
            response.setReasoning((String) review.get("reasoning"));
            return response;

        } catch (Exception e) {
            log.error("Failed to save log for plan {}", planId, e);
            LogResponse response = new LogResponse();
            response.setSuccess(false);
            response.setCoachNote("Session logged! Keep up the great work.");
            return response;
        }
    }

    public String generateShareCode(String planId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        if (plan.getShareCode() == null) {
            plan.setShareCode(generateId(8).toUpperCase());
            planRepository.save(plan);
        }
        return plan.getShareCode();
    }

    private PlanResponse toPlanResponse(TrainingPlan plan, boolean readOnly) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId());
        response.setAthleteName(plan.getAthleteName());
        response.setRaceDate(plan.getRaceDate());
        response.setRaceDistanceKm(plan.getRaceDistanceKm());
        response.setTargetTime(plan.getTargetTime());
        response.setFitnessLevel(plan.getFitnessLevel());
        response.setTotalWeeks(plan.getTotalWeeks());
        response.setShareCode(plan.getShareCode());
        response.setStravaAthleteId(plan.getStravaAthleteId());
        response.setReadOnly(readOnly);

        try {
            if (plan.getPlanJson() != null) {
                JsonNode planNode = objectMapper.readTree(plan.getPlanJson());
                JsonNode weeksNode = planNode.get("weeks");
                if (weeksNode != null && weeksNode.isArray()) {
                    List<JsonNode> weeks = new ArrayList<>();
                    for (JsonNode w : weeksNode) weeks.add(w);
                    response.setWeeks(weeks);
                }
            }
            if (plan.getLogsJson() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> logs = objectMapper.readValue(plan.getLogsJson(), Map.class);
                response.setLogs(logs);
            }
        } catch (Exception e) {
            log.error("Failed to parse plan JSON for {}", plan.getId(), e);
        }

        return response;
    }

    private String generateId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
