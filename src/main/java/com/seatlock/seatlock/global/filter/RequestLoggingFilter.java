package com.seatlock.seatlock.common.filter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put("requestId", requestId);

        log.info("요청 시작: {} {}", request.getMethod(), request.getRequestURI());

        try {
            filterChain.doFilter(request, response);

            log.info("요청 완료: 상태코드 {}", response.getStatus());
        } finally {
            MDC.clear();
        }


    }
}
