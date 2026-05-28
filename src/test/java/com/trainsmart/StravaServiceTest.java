package com.trainsmart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainsmart.entity.StravaActivity;
import com.trainsmart.entity.TrainingPlan;
import com.trainsmart.repository.StravaActivityRepository;
import com.trainsmart.repository.TrainingPlanRepository;
import com.trainsmart.service.StravaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StravaServiceTest {

    private StravaService service;
    private RestTemplate restTemplate;
    private TrainingPlanRepository planRepository;
    private StravaActivityRepository activityRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        planRepository = mock(TrainingPlanRepository.class);
        activityRepository = mock(StravaActivityRepository.class);
        service = new StravaService(restTemplate, objectMapper, planRepository, activityRepository);
        ReflectionTestUtils.setField(service, "clientId", "12345");
        ReflectionTestUtils.setField(service, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(service, "redirectUri", "http://localhost:8080/api/auth/strava/callback");
    }

    @Test
    void buildAuthUrl_includesClientIdAndRedirectUri() {
        String url = service.buildAuthUrl("plan123");
        assertThat(url).contains("client_id=12345");
        assertThat(url).contains("redirect_uri=http://localhost:8080/api/auth/strava/callback");
        assertThat(url).contains("state=plan123");
    }

    @Test
    void matchActivityToPlan_matchesCyclingToTuesdaySession() throws Exception {
        TrainingPlan plan = createTestPlan();
        StravaActivity activity = new StravaActivity();
        activity.setId(1L);
        activity.setActivityType("Ride");
        activity.setDistanceMeters(50000.0);

        // Match the service's week-1 Tuesday: Monday of plan-start week + 1 day
        java.time.LocalDate weekOneTuesday = java.time.LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY).plusDays(1);
        activity.setStartDate(weekOneTuesday.toString() + "T10:00:00Z");

        boolean matched = service.matchActivityToPlan(activity, plan);
        assertThat(matched).isTrue();
    }

    @Test
    void matchActivityToPlan_doesNotMatchRunToCyclingSession() throws Exception {
        TrainingPlan plan = createTestPlan();
        StravaActivity activity = new StravaActivity();
        activity.setId(2L);
        activity.setActivityType("Run");
        activity.setDistanceMeters(10000.0);
        activity.setStartDate(java.time.LocalDate.now().plusDays(1).toString() + "T07:00:00Z");

        boolean matched = service.matchActivityToPlan(activity, plan);
        assertThat(matched).isFalse();
    }

    @Test
    void matchActivityToPlan_doesNotMatchNonCyclingDay() throws Exception {
        TrainingPlan plan = createTestPlan();
        StravaActivity activity = new StravaActivity();
        activity.setId(3L);
        activity.setActivityType("Ride");
        activity.setDistanceMeters(50000.0);

        java.time.LocalDate monday = java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
        activity.setStartDate(monday.toString() + "T10:00:00Z");

        boolean matched = service.matchActivityToPlan(activity, plan);
        assertThat(matched).isFalse();
    }

    private TrainingPlan createTestPlan() throws Exception {
        String planJson = """
                {
                  "weeks": [
                    {
                      "week": 1,
                      "sessions": [
                        {"day": "Tue", "type": "Cycling", "label": "Easy spin", "zone": "easy"},
                        {"day": "Wed", "type": "Strength", "label": "Strength", "zone": "strength"},
                        {"day": "Thu", "type": "Cycling", "label": "Intervals", "zone": "interval"},
                        {"day": "Fri", "type": "Rest", "label": "Rest", "zone": "rest"},
                        {"day": "Sat", "type": "Cycling", "label": "Long ride", "zone": "long"},
                        {"day": "Sun", "type": "Mobility", "label": "Mobility", "zone": "mobility"}
                      ]
                    }
                  ]
                }
                """;
        TrainingPlan plan = new TrainingPlan();
        plan.setId("test123");
        plan.setPlanJson(planJson);
        plan.setRaceDate("2026-10-01");
        return plan;
    }
}
