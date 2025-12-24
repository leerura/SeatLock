package com.seatlock.seatlock.domain.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationRequestDTO(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotNull(message = "좌석 ID는 필수입니다.")
        Long seatId
) {
}
