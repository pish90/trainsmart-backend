package com.trainsmart.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogRequest {
    @NotBlank
    private String sessionKey;
    @NotBlank
    private String status;
    private Integer rpe;
    private String notes;
    private Integer week;
    private String day;
}
