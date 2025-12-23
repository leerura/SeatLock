package com.seatlock.seatlock.global.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.seatlock.seatlock.domain..*Service.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().toShortString();

        log.debug("[Service] 메서드 시작: {}", methodName);

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("[Service] 메서드 종료: {} | 실행 시간: {}ms", methodName, executionTime);

        return result;
    }

    @Around("execution(* com.seatlock.seatlock.domain..*Repository.*(..))")
    public Object logRepositoryExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("[Repository] 메서드 시작: {}", methodName);

        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;

        // 50ms 이상 걸리면 WARN 레벨로 출력 (느린 쿼리 경고)
        if (executionTime > 50) {
            log.warn("[Repository] 느린 쿼리 감지: {} | 실행 시간: {}ms", methodName, executionTime);
        } else {
            log.debug("[Repository] 메서드 종료: {} | 실행 시간: {}ms", methodName, executionTime);
        }

        return result;
    }
}
