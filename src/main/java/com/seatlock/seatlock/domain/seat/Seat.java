package com.seatlock.seatlock.domain.seat;

import com.seatlock.seatlock.common.BaseEntity;
import com.seatlock.seatlock.domain.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(
        name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_seat_number",
                        columnNames = {"event_id", "seat_number"}
                )
        }
)
@Getter
@NoArgsConstructor
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status;

    @Version
    @Column(name = "version")
    private Integer version;

    @Builder
    public Seat(Event event, String seatNumber, SeatStatus status) {
        this.event = event;
        this.seatNumber = seatNumber;
        this.status = status != null ? status : SeatStatus.AVAILABLE;
        this.version = 0;
    }

    public static Seat  of(Event event, String seatNumber) {
        return Seat.builder()
                .event(event)
                .seatNumber(seatNumber)
                .status(SeatStatus.AVAILABLE)
                .build();
    }

}
