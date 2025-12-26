package com.seatlock.seatlock.domain.event.entity;


import com.seatlock.seatlock.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /*
    @Version
    @Column(name = "version")
    private Integer version;
     */

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

    public void decreaseAvailableSeats() {
        if (this.availableSeats <= 0) {
            throw new IllegalStateException("예약 가능한 좌석이 없습니다.");
        }
        this.availableSeats--;
    }
}
