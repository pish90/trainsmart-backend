package com.trainsmart.dto;

import lombok.Data;

@Data
public class LogResponse {
    private boolean success;
    private String coachNote;
    private boolean planUpdated;
    private String reasoning;
}
