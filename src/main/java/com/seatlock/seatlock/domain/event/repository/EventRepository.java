package com.seatlock.seatlock.domain.event.repository;

import com.seatlock.seatlock.domain.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
