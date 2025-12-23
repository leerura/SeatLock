package com.seatlock.seatlock.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR("C001", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST("C002", "잘못된 요청입니다."),

    // User (U)
    USER_NOT_FOUND("U001", "존재하지 않는 사용자입니다."),

    // Seat (S)
    SEAT_NOT_FOUND("S001", "존재하지 않는 좌석입니다."),
    SEAT_ALREADY_RESERVED("S002", "이미 예약된 좌석입니다."),

    // Reservation (R)
    RESERVATION_NOT_FOUND("R001", "존재하지 않는 예약입니다."),
    ALREADY_RESERVED("R002", "이미 예약 내역이 존재합니다."),
    LOCK_ACQUISITION_FAILED("R003", "락 획득에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // Database (D)
    DATABASE_CONNECTION_ERROR("D001", "데이터베이스 연결에 실패했습니다.");


    private final String code;
    private final String message;
}
