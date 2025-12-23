package com.seatlock.seatlock.global.config;

import com.seatlock.seatlock.domain.event.entity.Event;
import com.seatlock.seatlock.domain.event.repository.EventRepository;
import com.seatlock.seatlock.domain.member.entity.Member;
import com.seatlock.seatlock.domain.member.repository.MemberRepository;
import com.seatlock.seatlock.domain.seat.entity.Seat;
import com.seatlock.seatlock.domain.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    private static final int TOTAL_MEMBERS = 100_000;
    private static final int TOTAL_SEATS = 1_000;
    private static final int BATCH_SIZE = 1_000;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("==================================================");
        log.info("더미 데이터 생성 시작");
        log.info("==================================================");

        // 기존 데이터 확인
        long memberCount = memberRepository.count();
        long eventCount = eventRepository.count();
        long seatCount = seatRepository.count();

        if (memberCount > 0 || eventCount > 0 || seatCount > 0) {
            log.info("이미 데이터가 존재합니다. 더미 데이터 생성을 건너뜁니다.");
            log.info("Member: {}, Event: {}, Seat: {}", memberCount, eventCount, seatCount);
            return;
        }

        long startTime = System.currentTimeMillis();

        // 1. Member 생성
        createMembers();

        // 2. Event 생성
        Event event = createEvent();

        // 3. Seat 생성
        createSeats(event);

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        log.info("==================================================");
        log.info("더미 데이터 생성 완료 (소요 시간: {}초)", duration);
        log.info("✅ Member: {}명", memberRepository.count());
        log.info("✅ Event: {}개", eventRepository.count());
        log.info("✅ Seat: {}개", seatRepository.count());
        log.info("==================================================");

    }

    private void createMembers() {
        log.info("Member {}명 생성 중...", TOTAL_MEMBERS);
        long startTime = System.currentTimeMillis();

        int batches = TOTAL_MEMBERS / BATCH_SIZE;
        for (int i = 0; i < batches; i++) {
            List<Member> memberBatch = new ArrayList<>();
            for (int j = 0; j < BATCH_SIZE; j++) {
                int memberNumber = i * BATCH_SIZE + j + 1;
                Member member = Member.of("user_" + memberNumber);
                memberBatch.add(member);
            }
            memberRepository.saveAll(memberBatch);
            memberRepository.flush();

            if ((i + 1) % 10 == 0) {
                log.info("Member 생성 진행 중... {}/{} 배치 완료", i + 1, batches);
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("✅ Member {}명 생성 완료 ({}초)", TOTAL_MEMBERS, (endTime - startTime) / 1000);
    }

    private Event createEvent() {
        log.info("Event 생성 중...");
        Event event = Event.of("2025 프리미엄 스포츠 경기", TOTAL_SEATS);
        eventRepository.save(event);
        log.info("✅ Event 생성 완료: {}", event.getEventName());
        return event;
    }

    private void createSeats(Event event) {
        log.info("Seat {}개 생성 중...", TOTAL_SEATS);
        long startTime = System.currentTimeMillis();

        List<Seat> allSeats = new ArrayList<>();

        // A등급: 250석 (A-001 ~ A-250)
        allSeats.addAll(createSeatsByGrade(event, "A", 250));

        // B등급: 250석 (B-001 ~ B-250)
        allSeats.addAll(createSeatsByGrade(event, "B", 250));

        // C등급: 250석 (C-001 ~ C-250)
        allSeats.addAll(createSeatsByGrade(event, "C", 250));

        // D등급: 250석 (D-001 ~ D-250)
        allSeats.addAll(createSeatsByGrade(event, "D", 250));

        // Batch Insert
        seatRepository.saveAll(allSeats);
        seatRepository.flush();

        long endTime = System.currentTimeMillis();
        log.info("✅ Seat {}개 생성 완료 ({}초)", TOTAL_SEATS, (endTime - startTime) / 1000);
    }


    private List<Seat> createSeatsByGrade(Event event, String grade, int count) {
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String seatNumber = String.format("%s-%03d", grade, i);
            Seat seat = Seat.of(event, seatNumber);
            seats.add(seat);
        }
        return seats;
    }

}
