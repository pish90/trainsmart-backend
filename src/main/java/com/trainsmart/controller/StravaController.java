package com.trainsmart.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainsmart.entity.StravaActivity;
import com.trainsmart.service.StravaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/strava")
@RequiredArgsConstructor
@Slf4j
public class StravaController {

    private final StravaService stravaService;
    private final ObjectMapper objectMapper;

    @Value("${strava.webhook.verify.token}")
    private String webhookVerifyToken;

    @GetMapping("/activities/{planId}")
    public ResponseEntity<List<StravaActivity>> syncActivities(@PathVariable String planId) {
        try {
            List<StravaActivity> activities = stravaService.syncActivities(planId);
            return ResponseEntity.ok(activities);
        } catch (RuntimeException e) {
            log.error("Sync error for plan {}", planId, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/webhook")
    public ResponseEntity<Map<String, String>> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken) {

        if ("subscribe".equals(mode) && webhookVerifyToken.equals(verifyToken)) {
            return ResponseEntity.ok(Map.of("hub.challenge", challenge));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            String objectType = event.path("object_type").asText("");
            String aspectType = event.path("aspect_type").asText("");
            String ownerId = event.path("owner_id").asText("");

            if ("activity".equals(objectType) && "create".equals(aspectType)) {
                log.info("Strava activity created for athlete {}", ownerId);
            }
        } catch (Exception e) {
            log.error("Webhook processing error", e);
        }
        return ResponseEntity.ok().build();
    }
}
