package com.seatlock.seatlock.domain.reservation.facade;


import com.seatlock.seatlock.domain.reservation.dto.ReservationResponseDTO;
import com.seatlock.seatlock.domain.reservation.service.ReservationService;
import com.seatlock.seatlock.global.exception.ErrorCode;
import com.seatlock.seatlock.global.exception.LockAcquisitionFailedException;
import com.seatlock.seatlock.global.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationFacade {

    private final ReservationService reservationService;
    private final LockManager lockManager;
    private final TransactionTemplate transactionTemplate;
    private final RedissonClient redissonClient;

    private static final long LOCK_TIMEOUT_MS = 100;

    /**
     * Seat Lock을 사용한 예약 생성
     *
     * TransactionTemplate을 사용하여 수동으로 트랜잭션 범위 제어
     * 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
     *
     * 근본적 해결: 락 범위 > 트랜잭션 범위로 Gap 완전 제거 But Event 수준의 Lock이 존재하지 않기 때문에 Event에서 Lost Update 발생
     */
    public ReservationResponseDTO createReservationWithSeatLock(Long memberId, Long seatId) {
        ReentrantLock lock = lockManager.getLock(seatId);

        log.debug("좌석 {}번 락 획득 시도 - memberId: {}", seatId, memberId);

        try {
            // 락 획득 시도
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("좌석 {}번 락 획득 실패 (타임아웃) - memberId: {}", seatId, memberId);
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.debug("좌석 {}번 락 획득 성공 - memberId: {}", seatId, memberId);

            try {
                // 트랜잭션 수동 관리 (락 획득 후 시작)
                return transactionTemplate.execute(status -> {
                    try {
                        return reservationService.createReservationWithoutTransaction(memberId, seatId);
                    } catch (Exception e) {
                        status.setRollbackOnly();  // 롤백 마킹
                        throw e;
                    }
                });
                // execute() 반환 시점 = 트랜잭션 커밋 완료 ✅

            } finally {
                // 트랜잭션 커밋 후 락 해제
                lock.unlock();
                log.debug("좌석 {}번 락 해제 (트랜잭션 커밋 후) - memberId: {}", seatId, memberId);
                lockManager.removeLock(seatId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("좌석 {}번 락 획득 중 인터럽트 발생 - memberId: {}", seatId, memberId, e);
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    /**
     * Java Lock을 사용한 예약 생성
     *
     * TransactionTemplate을 사용하여 수동으로 트랜잭션 범위 제어
     * 락 획득 → 트랜잭션 시작 → 비즈니스 로직 → 트랜잭션 커밋 → 락 해제
     *
     * 2단계 락 구조:
     * - Level 1 (Facade): Seat별 ReentrantLock - 같은 좌석 중복 방지
     * - Level 2 (Service): Event별 synchronized - Event.availableSeats Lost Update 방지
     *  Event에서의 Lost Update를 해결할 수 있으나 코드가 복잡하며 Dead Lock 발생 가능
     */
    public ReservationResponseDTO createReservationWithSeatEventLock(Long memberId, Long seatId) {
        ReentrantLock seatLock = lockManager.getLock(seatId);

        try {
            // Step 1: Seat Lock 획득
            boolean acquired = seatLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            try {
                // Step 2: Event Lock 객체 획득
                Long eventId = reservationService.getEventIdBySeatId(seatId);
                Object eventLock = lockManager.getEventLock(eventId);

                // Step 3: Event synchronized → 트랜잭션 → 비즈니스 로직
                synchronized (eventLock) {
                    log.debug("이벤트 {}번 synchronized 진입", eventId);

                    try {
                        // 트랜잭션 시작 (synchronized 안에서!)
                        return transactionTemplate.execute(status -> {
                            try {
                                return reservationService.createReservationWithoutTransaction(memberId, seatId);
                            } catch (Exception e) {
                                status.setRollbackOnly();
                                throw e;
                            }
                        });
                        // 트랜잭션 커밋 완료 (synchronized 안에서!)

                    } finally {
                        log.debug("이벤트 {}번 synchronized 종료 (커밋 완료)", eventId);
                    }
                }
                // ✅ synchronized 해제 = 트랜잭션 커밋 이후

            } finally {
                seatLock.unlock();
                lockManager.removeLock(seatId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }


    public ReservationResponseDTO createReservationWithSeatLockAndAtomicUpdate(Long memberId, Long seatId) {
        ReentrantLock lock = lockManager.getLock(seatId);

        log.debug("좌석 {}번 락 획득 시도 - memberId: {}", seatId, memberId);

        try {
            // 락 획득 시도
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("좌석 {}번 락 획득 실패 (타임아웃) - memberId: {}", seatId, memberId);
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.debug("좌석 {}번 락 획득 성공 - memberId: {}", seatId, memberId);

            try {
                // 트랜잭션 수동 관리 (락 획득 후 시작)
                return transactionTemplate.execute(status -> {
                    try {
                        return reservationService.createReservationWithoutTransactionAndAtomicUpdate(memberId, seatId);
                    } catch (Exception e) {
                        status.setRollbackOnly();  // 롤백 마킹
                        throw e;
                    }
                });
                // execute() 반환 시점 = 트랜잭션 커밋 완료 ✅

            } finally {
                // 트랜잭션 커밋 후 락 해제
                lock.unlock();
                log.debug("좌석 {}번 락 해제 (트랜잭션 커밋 후) - memberId: {}", seatId, memberId);
                //lockManager.removeLock(seatId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("좌석 {}번 락 획득 중 인터럽트 발생 - memberId: {}", seatId, memberId, e);
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    /**
     * Java Lock + DB Pessimistic Lock을 사용한 예약 생성 (멀티 서버 환경용)
     *
     * - Java Lock: 같은 서버 내에서 빠른 실패 (DB 접근 전 차단)
     * - DB Lock: 서버 간 동시성 제어 (SELECT FOR UPDATE)
     *
     * @param memberId 회원 ID
     * @param seatId 좌석 ID
     * @return 예약 응답 DTO
     */
    public ReservationResponseDTO createReservationWithDBLock(Long memberId, Long seatId) {
        ReentrantLock lock = lockManager.getLock(seatId);

        log.debug("좌석 {}번 락 획득 시도 (DB Lock) - memberId: {}", seatId, memberId);

        try {
            // Java Lock 획득 (같은 서버 내 빠른 실패)
            boolean acquired = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("좌석 {}번 락 획득 실패 (타임아웃) - memberId: {}", seatId, memberId);
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.debug("좌석 {}번 락 획득 성공 (DB Lock) - memberId: {}", seatId, memberId);

            try {
                // 트랜잭션 시작 (DB Lock은 트랜잭션 안에서 동작)
                return transactionTemplate.execute(status -> {
                    try {
                        return reservationService.createReservationWithDBLock(memberId, seatId);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });

            } finally {
                lock.unlock();
                log.debug("좌석 {}번 락 해제 (DB Lock, 트랜잭션 커밋 후) - memberId: {}", seatId, memberId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("좌석 {}번 락 획득 중 인터럽트 발생 - memberId: {}", seatId, memberId, e);
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    /**
     * Redis 분산 Lock을 사용한 예약 생성 (멀티 서버 환경용)
     *
     * @param memberId 회원 ID
     * @param seatId 좌석 ID
     * @return 예약 응답 DTO
     */
    public ReservationResponseDTO createReservationWithRedisLock(Long memberId, Long seatId) {
        String lockKey = "seat:lock:" + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        log.debug("좌석 {}번 Redis Lock 획득 시도 - memberId: {}", seatId, memberId);

        try {
            // Redis Lock 획득 (waitTime: 100ms, leaseTime: 5s)
            boolean acquired = lock.tryLock(100, 5000, TimeUnit.MILLISECONDS);

            if (!acquired) {
                log.warn("좌석 {}번 Redis Lock 획득 실패 (타임아웃) - memberId: {}", seatId, memberId);
                throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }

            log.debug("좌석 {}번 Redis Lock 획득 성공 - memberId: {}", seatId, memberId);

            try {
                // 트랜잭션 시작
                return transactionTemplate.execute(status -> {
                    try {
                        // 기존 Service 메서드 재사용
                        return reservationService.createReservationWithoutTransactionAndAtomicUpdate(memberId, seatId);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });

            } finally {
                // Lock 해제 (현재 스레드가 보유한 경우에만)
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("좌석 {}번 Redis Lock 해제 (트랜잭션 커밋 후) - memberId: {}", seatId, memberId);
                }
            }

        } catch (InterruptedException e) {
            // 인터럽트 발생 시 처리
            Thread.currentThread().interrupt();
            log.error("좌석 {}번 Redis Lock 획득 중 인터럽트 발생 - memberId: {}", seatId, memberId, e);
            throw new LockAcquisitionFailedException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }
}
