package com.seatlock.seatlock.domain.reservation.dto;

import com.seatlock.seatlock.domain.reservation.entity.Reservation;

import java.time.LocalDateTime;

public record ReservationResponseDTO(
        Long reservationId,
        Long memberId,
        Long seatId,
        Long eventId,
        LocalDateTime reservedAt
) {
    // 정적 팩토리 메서드
    public static ReservationResponseDTO from(Reservation reservation) {
        return new ReservationResponseDTO(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getSeatId(),
                reservation.getEventId(),
                reservation.getCreatedAt()  // BaseEntity의 createdAt
        );
    }
}
