package com.seatlock.seatlock.domain.health;


import com.seatlock.seatlock.global.ApiResponse;
import com.seatlock.seatlock.domain.health.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthCheckResponseDTO>> healthCheck() {
        HealthCheckResponseDTO data = HealthCheckResponseDTO.of("UP", "seat-lock");
        ApiResponse<HealthCheckResponseDTO> response = ApiResponse.success(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/healthz")
    public ResponseEntity<ApiResponse<HealthCheckResponseDTO>> healthz() {
        String status = healthCheckService.checkHealth();

        HealthCheckResponseDTO data = HealthCheckResponseDTO.of(status, "seat-lock");
        ApiResponse<HealthCheckResponseDTO> response = ApiResponse.success(data);
        return ResponseEntity.ok(response);
    }

}
