package com.seatlock.seatlock.health;


import com.seatlock.seatlock.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthCheckResponseDTO>> healthCheck() {
        HealthCheckResponseDTO data = HealthCheckResponseDTO.of("UP", "seat-lock");
        ApiResponse<HealthCheckResponseDTO> response = ApiResponse.success(data);
        return ResponseEntity.ok(response);
    }

}
