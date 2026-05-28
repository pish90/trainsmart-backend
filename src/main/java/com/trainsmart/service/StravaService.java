package com.trainsmart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainsmart.entity.StravaActivity;
import com.trainsmart.entity.TrainingPlan;
import com.trainsmart.repository.StravaActivityRepository;
import com.trainsmart.repository.TrainingPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StravaService {

    @Value("${strava.client.id}")
    private String clientId;

    @Value("${strava.client.secret}")
    private String clientSecret;

    @Value("${strava.redirect.uri}")
    private String redirectUri;

    private static final String STRAVA_AUTH_URL = "https://www.strava.com/oauth/authorize";
    private static final String STRAVA_TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String STRAVA_API_BASE = "https://www.strava.com/api/v3";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TrainingPlanRepository planRepository;
    private final StravaActivityRepository activityRepository;

    public String buildAuthUrl(String planId) {
        return STRAVA_AUTH_URL +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&approval_prompt=force" +
                "&scope=activity:read_all" +
                "&state=" + planId;
    }

    public TrainingPlan exchangeCodeForTokens(String code, String planId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(STRAVA_TOKEN_URL, entity, String.class);

        try {
            JsonNode tokenData = objectMapper.readTree(response.getBody());
            plan.setStravaAthleteId(tokenData.at("/athlete/id").asLong());
            plan.setStravaAccessToken(tokenData.get("access_token").asText());
            plan.setStravaRefreshToken(tokenData.get("refresh_token").asText());
            plan.setStravaTokenExpiry(tokenData.get("expires_at").asLong());
            return planRepository.save(plan);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }

    public void refreshTokenIfNeeded(TrainingPlan plan) {
        if (plan.getStravaTokenExpiry() == null) return;
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (plan.getStravaTokenExpiry() - nowSeconds > 300) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", plan.getStravaRefreshToken());
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(STRAVA_TOKEN_URL, entity, String.class);

        try {
            JsonNode tokenData = objectMapper.readTree(response.getBody());
            plan.setStravaAccessToken(tokenData.get("access_token").asText());
            plan.setStravaRefreshToken(tokenData.get("refresh_token").asText());
            plan.setStravaTokenExpiry(tokenData.get("expires_at").asLong());
            planRepository.save(plan);
        } catch (Exception e) {
            log.error("Failed to refresh Strava token for plan {}", plan.getId(), e);
        }
    }

    public List<StravaActivity> fetchRecentActivities(TrainingPlan plan) {
        refreshTokenIfNeeded(plan);

        long thirtyDaysAgo = (System.currentTimeMillis() / 1000) - (30L * 24 * 60 * 60);
        String url = STRAVA_API_BASE + "/athlete/activities?per_page=50&after=" + thirtyDaysAgo;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(plan.getStravaAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        List<StravaActivity> activities = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(response.getBody());
            for (JsonNode node : arr) {
                StravaActivity act = parseActivity(node, plan.getId());
                activities.add(act);
            }
        } catch (Exception e) {
            log.error("Failed to parse Strava activities", e);
        }
        return activities;
    }

    private StravaActivity parseActivity(JsonNode node, String planId) {
        StravaActivity act = new StravaActivity();
        act.setId(node.get("id").asLong());
        act.setPlanId(planId);
        act.setActivityName(node.path("name").asText(""));
        act.setActivityType(node.path("type").asText(""));
        act.setStartDate(node.path("start_date_local").asText(""));
        act.setDistanceMeters(node.path("distance").asDouble(0));
        act.setMovingTimeSeconds(node.path("moving_time").asInt(0));
        act.setElapsedTimeSeconds(node.path("elapsed_time").asInt(0));
        act.setAverageSpeedMps(node.path("average_speed").asDouble(0));
        if (node.has("average_watts")) act.setAverageWatts(node.get("average_watts").asDouble());
        if (node.has("average_heartrate")) act.setAverageHeartrate(node.get("average_heartrate").asDouble());
        act.setMatched(false);
        return act;
    }

    public boolean matchActivityToPlan(StravaActivity activity, TrainingPlan plan) {
        String type = activity.getActivityType();
        if (type == null || (!type.equalsIgnoreCase("Ride") && !type.equalsIgnoreCase("VirtualRide"))) {
            return false;
        }

        try {
            String startDate = activity.getStartDate();
            if (startDate == null || startDate.isBlank()) return false;
            LocalDate activityDate = LocalDate.parse(startDate.substring(0, 10));
            String dayOfWeek = activityDate.getDayOfWeek().name();

            boolean isCyclingDay = dayOfWeek.equals("TUESDAY") || dayOfWeek.equals("THURSDAY") || dayOfWeek.equals("SATURDAY");
            if (!isCyclingDay) return false;

            JsonNode planNode = objectMapper.readTree(plan.getPlanJson());
            JsonNode weeks = planNode.get("weeks");
            if (weeks == null) return false;

            LocalDate planStart = LocalDate.now();
            for (JsonNode week : weeks) {
                int weekNum = week.path("week").asInt(0);
                for (JsonNode session : week.path("sessions")) {
                    String sessionDay = session.path("day").asText("");
                    LocalDate sessionDate = getSessionDate(planStart, weekNum, sessionDay);
                    if (sessionDate != null && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(activityDate, sessionDate)) <= 1) {
                        activity.setMatched(true);
                        activity.setMatchedWeek(weekNum);
                        activity.setMatchedDay(sessionDay);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error matching activity to plan", e);
        }
        return false;
    }

    private LocalDate getSessionDate(LocalDate planStart, int weekNum, String day) {
        int offset = switch (day) {
            case "Tue" -> 1;
            case "Thu" -> 3;
            case "Sat" -> 5;
            default -> -1;
        };
        if (offset < 0) return null;
        return planStart.plusWeeks(weekNum - 1).with(java.time.DayOfWeek.MONDAY).plusDays(offset);
    }

    public List<StravaActivity> syncActivities(String planId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        if (plan.getStravaAccessToken() == null) {
            throw new RuntimeException("Strava not connected for plan: " + planId);
        }

        List<StravaActivity> activities = fetchRecentActivities(plan);
        for (StravaActivity activity : activities) {
            matchActivityToPlan(activity, plan);
            activityRepository.save(activity);
        }
        return activities;
    }
}
