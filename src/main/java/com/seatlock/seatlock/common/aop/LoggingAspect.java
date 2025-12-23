package com.seatlock.seatlock.common.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.seatlock.seatlock.domain..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().toShortString();

        log.debug("메서드 시작: {}", methodName);

        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("메서드 종료: {} | 실행 시간: {}ms", methodName, executionTime);

        return result;
    }
}
