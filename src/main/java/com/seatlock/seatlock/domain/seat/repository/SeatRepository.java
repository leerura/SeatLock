package com.seatlock.seatlock.domain.seat.repository;

import com.seatlock.seatlock.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}
