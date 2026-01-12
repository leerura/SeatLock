package com.seatlock.seatlock.domain.reservation.service;

import com.seatlock.seatlock.domain.event.repository.EventRepository;
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

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;

    private final ConcurrentHashMap<Long, Object> eventLocks = new ConcurrentHashMap<>();

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

    /**
     * Java Lock (ReentrantLock)용 예약 메서드
     * Java Lock (ReentrantLock)용 예약 메서드
     */
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

    /**
     * Java Lock + Event Synchronized용 예약 메서드
     * Facade에서 Seat별 락 + Service에서 Event별 synchronized
     */
    // ✅ 추가: synchronized 블록 내 변경사항을 DB에 반영
    public ReservationResponseDTO createReservationWithEventLock(Long memberId, Long seatId) {
        log.info("예약 시작 (Event Lock) - memberId: {}, seatId: {}", memberId, seatId);

        // 1. 좌석 조회
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));


        Long eventId = seat.getEvent().getId();

        // 2. Event별 락 객체 가져오기 (없으면 생성)
        Object eventLock = eventLocks.computeIfAbsent(eventId, id -> {
            log.debug("이벤트 {}번에 대한 새로운 synchronized 락 생성", id);
            return new Object();
        });

        // 3. Event별로 synchronized (availableSeats Lost Update 방지)
        synchronized (eventLock) {
            log.debug("이벤트 {}번 synchronized 블록 진입 - memberId: {}, seatId: {}",
                    eventId, memberId, seatId);

            Seat lockedSeat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

            Event event = lockedSeat.getEvent();

            // 좌석 예약 가능 여부 확인
            if (!lockedSeat.isAvailable()) {
                throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
            }

            // 중복 예약 체크
            if (reservationRepository.existsByMemberIdAndEventId(memberId, eventId)) {
                throw new CustomException(ErrorCode.ALREADY_RESERVED_THIS_EVENT);
            }

            // 좌석 예약 처리
            lockedSeat.reserve();

            // Event의 availableSeats 감소 (synchronized로 보호됨!)
            event.decreaseAvailableSeats();

            // 예약 생성
            Reservation reservation = Reservation.of(memberId, seatId, eventId);
            reservationRepository.save(reservation);

            log.info("예약 완료 (Event Lock) - reservationId: {}", reservation.getId());
            log.debug("이벤트 {}번 synchronized 블록 종료 - memberId: {}, seatId: {}",
                    eventId, memberId, seatId);

            return ReservationResponseDTO.from(reservation);
        }
    }

    // 트랜잭션 내부에서 실행되는 메서드
    public ReservationResponseDTO createReservationWithoutTransactionAndAtomicUpdate(Long memberId, Long seatId) {

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));

        if (!seat.isAvailable()) { // 이미 Java Lock 안이라 안전하지만 더블 체크
            throw new CustomException(ErrorCode.SEAT_ALREADY_RESERVED);
        }

        Event event = seat.getEvent();

        if (reservationRepository.existsByMemberIdAndEventId(memberId, event.getId())) {
            throw new CustomException(ErrorCode.ALREADY_RESERVED_THIS_EVENT);
        }

        // [핵심 변경] Event의 availableSeats 감소를 Java 객체 수정이 아닌 DB 쿼리로 직접 수행

        int updatedRows = eventRepository.decreaseAvailableSeats(event.getId());
        if (updatedRows == 0) {
            throw new CustomException(ErrorCode.EVENT_SOLD_OUT); // 잔여석이 0이라 업데이트 실패 시
        }

        seat.reserve();

        seatRepository.save(seat);

        // 6. 예약 생성 (기존 유지)
        Reservation reservation = Reservation.of(memberId, seatId, event.getId());
        reservationRepository.save(reservation);

        log.info("예약 완료 (Event Lock) - reservationId: {}", reservation.getId());

        return ReservationResponseDTO.from(reservation);
    }



    public Long getEventIdBySeatId(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new CustomException(ErrorCode.SEAT_NOT_FOUND));
        return seat.getEvent().getId();
    }


}