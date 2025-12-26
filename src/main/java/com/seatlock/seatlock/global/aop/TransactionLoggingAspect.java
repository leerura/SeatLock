package com.seatlock.seatlock.global.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class TransactionLoggingAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logTransaction(ProceedingJoinPoint joinPoint) throws Throwable {

        // 1. 트랜잭션 ID 생성
        String txId = "tx-" + UUID.randomUUID().toString().substring(0, 8);

        // 2. MDC에 저장
        MDC.put("txId", txId);

        try {
            log.debug("[Transaction] 시작: {}", txId);

            // 3. 실제 트랜잭션 메서드 실행
            Object result = joinPoint.proceed();

            log.debug("[Transaction] 커밋: {}", txId);

            return result;

        } catch (Throwable t) {
            // 예외는 GlobalExceptionHandler에서 로깅하므로 여기서는 debug 레벨로
            log.debug("[Transaction] 롤백: {} | 에러: {}", txId, t.getMessage());
            throw t;

        } finally {
            // 4. MDC에서 제거 (중요!)
            MDC.remove("txId");
        }
    }
}
