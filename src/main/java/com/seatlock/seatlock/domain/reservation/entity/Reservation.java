package com.seatlock.seatlock.domain.reservation.entity;

import com.seatlock.seatlock.global.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "event_id"}),
        @UniqueConstraint(columnNames = {"seat_id"})
})
@Getter
@NoArgsConstructor
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;


    @Builder
    public Reservation(Long memberId, Long seatId, Long eventId) {
        this.memberId = memberId;
        this.seatId = seatId;
        this.eventId = eventId;
    }

    // 정적 팩토리 메서드
    public static Reservation of(Long memberId, Long seatId, Long eventId) {
        return Reservation.builder()
                .memberId(memberId)
                .seatId(seatId)
                .eventId(eventId)
                .build();
    }
}
