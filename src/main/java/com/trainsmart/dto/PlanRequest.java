package com.trainsmart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanRequest {
    @NotBlank
    private String athleteName;
    @NotBlank
    private String raceDate;
    @NotNull
    private Integer raceDistanceKm;
    @NotBlank
    private String targetTime;
    @NotBlank
    private String fitnessLevel;
}
