package com.seatlock.seatlock.domain.event;


import com.seatlock.seatlock.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Auditable;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Builder
    public Event(String eventName, Integer totalSeats, Integer availableSeats) {
        this.eventName = eventName;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }

    public static Event of(String eventName, Integer totalSeats) {
        return Event.builder()
                .eventName(eventName)
                .totalSeats(totalSeats)
                .availableSeats(totalSeats) // 초기값은 전체 좌석 수와 동일
                .build();
    }
}
