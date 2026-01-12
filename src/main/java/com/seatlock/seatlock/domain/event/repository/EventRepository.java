package com.seatlock.seatlock.domain.event.repository;

import com.seatlock.seatlock.domain.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {


    @Modifying(clearAutomatically = true, flushAutomatically = false)
    @Query(value = "UPDATE events SET available_seats = available_seats - 1 WHERE id = :eventId AND available_seats > 0", nativeQuery = true)
    int decreaseAvailableSeats(@Param("eventId") Long eventId);
}
