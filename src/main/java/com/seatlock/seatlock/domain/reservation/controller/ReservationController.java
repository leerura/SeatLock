package com.seatlock.seatlock.domain.reservation.controller;

import com.seatlock.seatlock.domain.reservation.facade.ReservationFacade;
import com.seatlock.seatlock.global.ApiResponse;
import com.seatlock.seatlock.domain.reservation.dto.ReservationRequestDTO;
import com.seatlock.seatlock.domain.reservation.dto.ReservationResponseDTO;
import com.seatlock.seatlock.domain.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationFacade reservationFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> createReservation(
            @Valid @RequestBody ReservationRequestDTO request
    ) {
        log.info("예약 요청 - memberId: {}, seatId: {}", request.memberId(), request.seatId());

        ReservationResponseDTO response = reservationService.createReservation(
                request.memberId(),
                request.seatId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/java-lock")
    public ResponseEntity<ApiResponse<ReservationResponseDTO>> createReservationWithJavaLock(
            @Valid @RequestBody ReservationRequestDTO request
    ) {
        log.info("[Java Lock] 예약 요청 - memberId: {}, seatId: {}", request.memberId(), request.seatId());

        ReservationResponseDTO response = reservationFacade.createReservationWithLock(
                request.memberId(),
                request.seatId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

}