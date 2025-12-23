package com.seatlock.seatlock.domain.reservation.repository;

import com.seatlock.seatlock.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByMemberIdAndEventId(Long seatNumber, Long eventId);
}
