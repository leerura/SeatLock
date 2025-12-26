package com.seatlock.seatlock.domain.reservation.service;

import com.seatlock.seatlock.global.exception.CustomException;
import com.seatlock.seatlock.global.exception.ErrorCode;
import com.seatlock.seatlock.domain.event.entity.Event;
import com.seatlock.seatlock.domain.reservation.dto.ReservationResponseDTO;
import com.seatlock.seatlock.domain.reservation.entity.Reservation;
import com.seatlock.seatlock.domain.reservation.repository.ReservationRepository;
import com.seatlock.seatlock.domain.seat.entity.Seat;
import com.seatlock.seatlock.domain.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public ReservationResponseDTO createReservation(Long memberId, Long seatId) {
        log.info("예약 시작 - memberId: {}, seatId: {}", memberId, seatId);

        // 1. 좌석 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        // 2. 좌석 예약 가능 여부 확인
        if (!seat.isAvailable()) {
            throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
        }


        Event event = seat.getEvent();
        // 3. 중복 예약 체크 (같은 이벤트에 이미 예약했는지)
        Long eventId = event.getId();
        if (reservationRepository.existsByMemberIdAndEventId(memberId, eventId)) {
            throw new CustomException(ErrorCode.ALREADY_RESERVED_THIS_EVENT);
        }

        // 4. 좌석 예약 처리 (AVAILABLE → RESERVED)
        seat.reserve();

        // 5. Event의 availableSeats 감소
        event.decreaseAvailableSeats();

        // 6. 예약 생성
        Reservation reservation = Reservation.of(memberId, seatId, eventId);
        reservationRepository.save(reservation);

        log.info("예약 완료 - reservationId: {}", reservation.getId());

        return ReservationResponseDTO.from(reservation);
    }

    // Java Lock용 새 메서드 (트랜잭션 없음)
    public ReservationResponseDTO createReservationWithoutTransaction(Long memberId, Long seatId) {
        log.info("예약 시작 - memberId: {}, seatId: {}", memberId, seatId);

        // 1. 좌석 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        // 2. 좌석 예약 가능 여부 확인
        if (!seat.isAvailable()) {
            throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
        }

        Event event = seat.getEvent();

        // 3. 중복 예약 체크
        Long eventId = event.getId();
        if (reservationRepository.existsByMemberIdAndEventId(memberId, eventId)) {
            throw new CustomException(ErrorCode.ALREADY_RESERVED_THIS_EVENT);
        }

        // 4. 좌석 예약 처리
        seat.reserve();

        // 5. Event의 availableSeats 감소
        event.decreaseAvailableSeats();

        // 6. 예약 생성
        Reservation reservation = Reservation.of(memberId, seatId, eventId);
        reservationRepository.save(reservation);

        log.info("예약 완료 - reservationId: {}", reservation.getId());

        return ReservationResponseDTO.from(reservation);
    }


}