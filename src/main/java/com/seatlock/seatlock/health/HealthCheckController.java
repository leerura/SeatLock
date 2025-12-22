package com.seatlock.seatlock.health;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponseDTO> healthCheck() {
        HealthCheckResponseDTO response = HealthCheckResponseDTO.of("UP", "seat-lock");
        return ResponseEntity.ok(response);
    }

}
