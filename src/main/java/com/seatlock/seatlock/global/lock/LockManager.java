package com.seatlock.seatlock.global.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class LockManager {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * 좌석 ID에 해당하는 락을 반환
     * 없으면 새로 생성하여 반환 (computeIfAbsent로 원자적 생성)
     *
     * @param seatId 좌석 ID
     * @return ReentrantLock
     */
    public ReentrantLock getLock(Long seatId) {
        return locks.computeIfAbsent(seatId, id -> {
            log.debug("좌석 {}번에 대한 새로운 락 생성", id);
            return new ReentrantLock(true); // fair lock (공정성 보장)
        });
    }

    /**
     * 현재 관리 중인 락의 개수 반환 (모니터링용)
     *
     * @return 락 개수
     */
    public int getLockCount() {
        return locks.size();
    }

}
