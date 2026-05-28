package com.trainsmart.controller;

import com.trainsmart.entity.TrainingPlan;
import com.trainsmart.service.StravaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final StravaService stravaService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/strava")
    public ResponseEntity<Void> initiateStravaAuth(@RequestParam String planId) {
        String authUrl = stravaService.buildAuthUrl(planId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    @GetMapping("/strava/callback")
    public ResponseEntity<Void> stravaCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            TrainingPlan plan = stravaService.exchangeCodeForTokens(code, state);
            String redirectUrl = frontendUrl + "/dashboard/" + plan.getId() + "?strava=connected";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        } catch (Exception e) {
            log.error("Strava callback error", e);
            String errorUrl = frontendUrl + "?error=strava_auth_failed";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(errorUrl))
                    .build();
        }
    }
}
