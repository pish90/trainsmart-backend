package com.trainsmart.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PlanResponse {
    private String id;
    private String athleteName;
    private String raceDate;
    private Integer raceDistanceKm;
    private String targetTime;
    private String fitnessLevel;
    private Integer totalWeeks;
    private List<JsonNode> weeks;
    private Map<String, Object> logs;
    private String shareCode;
    private Long stravaAthleteId;
    private boolean readOnly;
}
