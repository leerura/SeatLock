package com.seatlock.seatlock.domain.reservation.facade;


import com.seatlock.seatlock.domain.reservation.dto.ReservationResponseDTO;
import com.seatlock.seatlock.domain.reservation.service.ReservationService;
import com.seatlock.seatlock.global.exception.ErrorCode;
import com.seatlock.seatlock.global.exception.LockAcquisitionFailedException;
import com.seatlock.seatlock.global.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ReservationService reservationService;
    private final LockManager lockManager;

    private static final long LOCK_TIMEOUT_SECONDS = 5;

    /**
     * Java Lock을 사용한 예약 생성
     *
     * @Transactional을 Facade에 적용하여 락 범위와 트랜잭션 범위를 일치시킴
     * 락 해제는 트랜잭션 커밋 이후에 발생하도록 보장
     * @param memberId 회원 ID
     * @param seatId 좌석 ID
     * @return 예약 응답 DTO
     * @throws LockAcquisitionFailedException 락 획득 실패 시
     */
    @Transactional
    public ReservationResponseDTO createReservationWithLock(Long memberId, Long seatId) {
        ReentrantLock lock = lockManager.getLock(seatId);

        log.debug("좌석 {}번 락 획득 시도 - memberId: {}", seatId, memberId);

        try {
            // 락 획득 시도 (5초 타임아웃)
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                log.warn("좌석 {}번 락 획득 실패 (타임아웃) - memberId: {}", seatId, memberId);
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.debug("좌석 {}번 락 획득 성공 - memberId: {}", seatId, memberId);

            try {
                // 실제 예약 로직 실행
                return reservationService.createReservationWithoutTransaction(memberId, seatId);
            } finally {
                // 락 해제 (반드시 실행)
                lock.unlock();
                log.debug("좌석 {}번 락 해제 - memberId: {}", seatId, memberId);

                lockManager.removeLock(seatId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("좌석 {}번 락 획득 중 인터럽트 발생 - memberId: {}", seatId, memberId, e);
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

}
