package com.seatlock.seatlock.health;

public record HealthCheckResponseDTO(
        String status,
        String service
) {
    public static HealthCheckResponseDTO of(String status, String service) {
        return new HealthCheckResponseDTO(status, service);
    }
}
