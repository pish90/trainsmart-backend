package com.trainsmart.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_plans")
@Data
@NoArgsConstructor
public class TrainingPlan {

    @Id
    @Column(length = 16)
    private String id;

    @Column(nullable = false)
    private String athleteName;

    @Column(nullable = false)
    private String raceDate;

    private Integer raceDistanceKm;
    private String targetTime;
    private String fitnessLevel;
    private Integer totalWeeks;

    @Column(columnDefinition = "TEXT")
    private String planJson;

    @Column(columnDefinition = "TEXT")
    private String logsJson;

    @Column(unique = true)
    private String shareCode;

    private Long stravaAthleteId;
    private String stravaAccessToken;
    private String stravaRefreshToken;
    private Long stravaTokenExpiry;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
