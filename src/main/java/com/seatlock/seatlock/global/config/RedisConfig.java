package com.seatlock.seatlock.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Single Server 모드 설정
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionPoolSize(50)          // 커넥션 풀 크기
                .setConnectionMinimumIdleSize(10)   // 최소 유휴 커넥션
                .setIdleConnectionTimeout(10000)    // 유휴 커넥션 타임아웃 (10초)
                .setConnectTimeout(3000)            // 연결 타임아웃 (3초)
                .setTimeout(3000)                   // 명령 타임아웃 (3초)
                .setRetryAttempts(3)                // 재시도 횟수
                .setRetryInterval(1500);            // 재시도 간격 (1.5초)

        return Redisson.create(config);
    }



}
