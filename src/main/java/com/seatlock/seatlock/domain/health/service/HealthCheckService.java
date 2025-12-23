package com.seatlock.seatlock.domain.health.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HealthCheckService {

    public String checkHealth() {
        log.debug("헬스체크 서비스 실행 중...");

        // 시간이 걸리는 작업 시뮬레이션
        try {
            Thread.sleep(100);  // 0.1초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "OK";
    }
}
