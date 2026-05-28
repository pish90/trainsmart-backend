package com.trainsmart.controller;

import com.trainsmart.dto.LogRequest;
import com.trainsmart.dto.LogResponse;
import com.trainsmart.dto.PlanRequest;
import com.trainsmart.dto.PlanResponse;
import com.trainsmart.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.createPlan(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable String id) {
        try {
            return ResponseEntity.ok(planService.getPlan(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/logs")
    public ResponseEntity<LogResponse> saveLog(@PathVariable String id, @Valid @RequestBody LogRequest request) {
        try {
            return ResponseEntity.ok(planService.saveLog(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, String>> generateShareCode(@PathVariable String id) {
        try {
            String code = planService.generateShareCode(id);
            return ResponseEntity.ok(Map.of("shareCode", code));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/share/{code}")
    public ResponseEntity<PlanResponse> getSharedPlan(@PathVariable String code) {
        try {
            return ResponseEntity.ok(planService.getSharedPlan(code));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
