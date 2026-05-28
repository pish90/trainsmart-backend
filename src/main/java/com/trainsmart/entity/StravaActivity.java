package com.trainsmart.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "strava_activities")
@Data
@NoArgsConstructor
public class StravaActivity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String planId;

    private String activityName;
    private String activityType;
    private String startDate;
    private Double distanceMeters;
    private Integer movingTimeSeconds;
    private Integer elapsedTimeSeconds;
    private Double averageSpeedMps;
    private Double averageWatts;
    private Double averageHeartrate;

    private Boolean matched;
    private Integer matchedWeek;
    private String matchedDay;
}
