package com.seatlock.seatlock.domain.seat;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeatStatus {
    AVAILABLE("예약 가능"),
    RESERVED("예약됨");

    private final String description;
}
