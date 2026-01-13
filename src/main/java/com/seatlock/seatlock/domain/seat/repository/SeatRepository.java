package com.seatlock.seatlock.domain.seat.repository;

import com.seatlock.seatlock.domain.seat.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * 비관적 락을 사용하여 좌석 조회 (멀티 서버 환경용)
     * SELECT ... FOR UPDATE 쿼리 실행
     *
     * @param seatId 좌석 ID
     * @return 비관적 락이 걸린 Seat 엔티티
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);
}
