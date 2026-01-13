# 🎫 고가용성 예매 시스템 - 동시성 제어 메커니즘 비교

> 대규모 트래픽 환경에서 발생하는 동시성 문제를 해결하기 위한 4가지 메커니즘의 성능 및 효과 비교 분석

[![Java](https://img.shields.io/badge/Java-17-red?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue?logo=docker)](https://www.docker.com/)

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [기술 스택](#-기술-스택)
- [테스트 환경](#-테스트-환경)
- [동시성 제어 메커니즘](#-동시성-제어-메커니즘)
- [테스트 결과 비교](#-테스트-결과-비교)
- [핵심 인사이트](#-핵심-인사이트)
- [실행 방법](#-실행-방법)
- [API 명세](#-api-명세)

---

## 🎯 프로젝트 개요

### 문제 정의

대규모 동시 접속 상황에서 발생하는 **동시성 문제**:
- **오버부킹(Overbooking)**: 동일한 좌석에 중복 예약 발생
- **Lost Update**: Event의 `availableSeats` 감소분 누락

### 해결 목표

4가지 동시성 제어 메커니즘을 구현하고 성능 비교를 통해 최적의 솔루션 도출

---

## 🛠 기술 스택

### Backend
- **Java 17** - LTS 버전
- **Spring Boot 3.4.1** - 웹 프레임워크
- **Spring Data JPA** - ORM
- **MySQL 8.0** - 관계형 데이터베이스
- **Redis 7.0** - 분산 Lock용 인메모리 DB
- **Redisson 3.27.0** - Redis 분산 Lock 라이브러리

### Infrastructure
- **Docker** - 컨테이너화
- **Docker Compose** - 멀티 서버 환경 구성
- **Nginx** - 로드 밸런서 (Round-Robin)

### Testing
- **Apache JMeter** - 부하 테스트 도구

---

## 🧪 테스트 환경

### 시스템 구성

#### 싱글 서버
```
Client → Spring Boot (8080) → MySQL
```

#### 멀티 서버
```
Client → Nginx (80) → [Spring Boot × 3] → MySQL, Redis
                      (8081, 8082, 8083)
```

### 테스트 시나리오

| 시나리오 | 좌석 수 | 동시 요청 | 예상 결과 |
|---------|--------|----------|----------|
| **S1** | 1개 | 100명/1초 | 예약 1건 |
| **S2** | 10개 | 1,000명/1초 | 예약 10건 |
| **S3** | 10개 | 10,000명/1초 | 예약 10건 |

---

## 🔐 동시성 제어 메커니즘

### 1. @Transactional 어노테이션만 사용 ❌

**구현**
```java
@Transactional
public ReservationResponseDTO createReservation(Long memberId, Long seatId) {
    Seat seat = seatRepository.findById(seatId);
    seat.reserve();
    event.decreaseAvailableSeats();
}
```

**결과**

| 시나리오 | 예약 건수 | 오버부킹 | Event Lost Update |
|---------|----------|---------|------------------|
| S1 | 10건 | ⚠️ 9건 초과 | 1만 감소 (9건 누락) |
| S2 | 22건 | ⚠️ 12건 초과 | 987 (9건 누락) |
| S3 | 14건 | ⚠️ 4건 초과 | 997 (7건 누락) |

**문제점**
- 트랜잭션 격리 수준(REPEATABLE READ)으로 동시성 제어 불가
- Seat 오버부킹 발생
- Event Lost Update 발생

---

### 2. Java 수준 Lock (ReentrantLock) ⚠️

**구현**
```java
public ReservationResponseDTO createReservation(Long memberId, Long seatId) {
    ReentrantLock lock = lockManager.getLock(seatId);
    lock.lock();
    try {
        // 비즈니스 로직
        eventRepository.decreaseAvailableSeats(eventId); // Atomic UPDATE
    } finally {
        lock.unlock();
    }
}
```

**싱글 서버 결과**

| 시나리오 | 예약 건수 | 오버부킹 | Lost Update |
|---------|----------|---------|-------------|
| S1 | 1건 ✅ | 없음 | 없음 |
| S2 | 10건 ✅ | 없음 | 없음 |
| S3 | 10건 ✅ | 없음 | 없음 |

**멀티 서버 결과**

| 시나리오 | 예약 건수 | 오버부킹 |
|---------|----------|---------|
| S1 | 3건 | ⚠️ 2건 초과 |
| S2 | 30건 | ⚠️ 20건 초과 |
| S3 | 30건 | ⚠️ 20건 초과 |

**문제점**
- Java Lock은 JVM 내부에서만 동작
- 각 서버가 독립적인 Lock 객체 보유 → 서버 간 동시 접근 허용
- **Event Lost Update는 Atomic UPDATE로 해결**

---

### 3. DB 비관적 락 (Pessimistic Lock) ✅

**구현**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.id = :seatId")
Optional<Seat> findByIdWithLock(@Param("seatId") Long seatId);

// 생성되는 SQL: SELECT * FROM seats WHERE id = ? FOR UPDATE
```

**결과**

| 시나리오 | 예약 건수 | 오버부킹 | 평균 응답시간 | TPS |
|---------|----------|---------|--------------|-----|
| S1 | 1건 ✅ | 없음 | 111ms | 99.8 |
| S2 | 10건 ✅ | 없음 | 670ms | 464.3 |
| S3 | 10건 ✅ | 없음 | - | - |

**장점**
- ✅ 멀티 서버 환경에서도 완벽한 동시성 제어
- ✅ 네트워크 오버헤드 없음 (DB 내부 처리)
- ✅ Lock 해제 시 대기 중인 요청 즉시 처리

**단점**
- ⚠️ DB 커넥션 점유 시간 증가
- ⚠️ Lock 경쟁이 심할수록 대기 시간 증가

---

### 4. Redis 분산 락 (Redisson) ⚠️

**구현**
```java
RLock lock = redissonClient.getLock("seat:lock:" + seatId);
boolean acquired = lock.tryLock(100, 3000, TimeUnit.MILLISECONDS);
try {
    // 비즈니스 로직
} finally {
    lock.unlock();
}
```

**결과**

| 시나리오 | 예약 건수 | 오버부킹 | 평균 응답시간 | TPS |
|---------|----------|---------|--------------|-----|
| S1 | 1건 ✅ | 없음 | 711ms | 61.3 |
| S2 | 10건 ✅ | 없음 | 1,694ms | 207.2 |
| S3 | 10건 ✅ | 없음 | - | - |

**성능 비교 (S2 기준)**
- **DB Lock**: 670ms, 464.3 TPS
- **Redis Lock**: 1,694ms, 207.2 TPS
- **결과**: DB Lock이 **2.5배 빠름**, TPS **2.2배 높음**

**Redis가 느린 이유**

1. **네트워크 오버헤드**
```
Lock 획득: 2ms (네트워크)
비즈니스 로직: 10ms
Lock 해제: 2ms (네트워크)
총 오버헤드: 4ms × 1,000명 = 4,000ms
```

2. **배보다 배꼽이 큰 상황**
```
실제 트랜잭션: ~10ms
Redis 오버헤드: ~4ms (40%)
→ 트랜잭션이 빠를수록 오버헤드 비율 증가
```

---

## 📊 테스트 결과 비교

### 성능 비교표

| 메커니즘 | 싱글 서버 | 멀티 서버 | S2 응답시간 | S2 TPS | 권장도 |
|---------|----------|----------|------------|--------|--------|
| @Transactional | ❌ | ❌ | - | - | ⛔ |
| Java Lock | ✅ | ❌ | - | - | ⚠️ |
| **DB 비관적 락** | ✅ | ✅ | **670ms** | **464.3** | ⭐⭐⭐⭐⭐ |
| Redis 분산 락 | ✅ | ✅ | 1,694ms | 207.2 | ⚠️ |

---

## 💡 핵심 인사이트

### "Redis 분산 락이 항상 정답은 아니다"

#### 본 프로젝트의 선택: DB 비관적 락

**선택 이유**
1. ✅ 트랜잭션 소요 시간이 짧음 (~10ms)
2. ✅ 네트워크 오버헤드 없음
3. ✅ 멀티 서버 환경에서 완벽한 동시성 제어
4. ✅ Redis 대비 2-3배 빠른 성능
5. ✅ 추가 인프라(Redis) 불필요

#### Redis 분산 락이 적합한 경우

| 조건 | 설명 |
|-----|------|
| **긴 비즈니스 로직** | 100ms 이상 소요 시 네트워크 오버헤드 상대적으로 감소 |
| **DB Lock 불가** | 여러 DB 간 분산 트랜잭션, DB 외부 자원 제어 |
| **광범위한 분산** | 10,000명 → 1,000개 좌석 (Lock 경쟁 낮음) |

### 트레이드오프 이해

| 특성 | DB Lock | Redis Lock |
|-----|---------|-----------|
| **성능** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **확장성** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **복잡도** | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **인프라** | DB만 | DB + Redis |

---

## 🚀 실행 방법

### 사전 요구사항

- Java 17+
- MySQL 8.0+
- Redis 7.0+ (Redis Lock 테스트 시)
- Docker & Docker Compose (멀티 서버 테스트 시)





## 📡 API 명세

### 예약 생성 엔드포인트

| 엔드포인트 | 설명 | 싱글 | 멀티 |
|-----------|------|------|------|
| `POST /api/v1/reservations` | @Transactional만 | ❌ | ❌ |
| `POST /api/v1/reservations/seat-lock` | Java Lock | ✅ | ❌ |
| `POST /api/v1/reservations/seat-db-lock` | DB 비관적 락 | ✅ | ✅ |
| `POST /api/v1/reservations/redis-lock` | Redis 분산 락 | ✅ | ✅ |

### 요청 예시
```bash
POST /api/v1/reservations/seat-db-lock
Content-Type: application/json

{
  "memberId": 1,
  "seatId": 1
}
```

### 응답 예시
```json
{
  "success": true,
  "data": {
    "reservationId": 1,
    "memberId": 1,
    "seatId": 1,
    "eventId": 1,
    "createdAt": "2025-01-13T12:00:00"
  }
}
```

---

## 📁 프로젝트 구조
```
seat-lock/
├── src/
│   ├── main/
│   │   ├── java/com/seatlock/seatlock/
│   │   │   ├── domain/
│   │   │   │   ├── event/          # Event 엔티티 및 Repository
│   │   │   │   ├── seat/           # Seat 엔티티 및 Repository
│   │   │   │   └── reservation/    # 예약 도메인
│   │   │   │       ├── controller/ # API 컨트롤러
│   │   │   │       ├── service/    # 비즈니스 로직
│   │   │   │       ├── facade/     # Lock 처리 계층
│   │   │   │       └── dto/        # 요청/응답 DTO
│   │   │   ├── global/
│   │   │   │   ├── config/         # Redis, DB 설정
│   │   │   │   ├── exception/      # 예외 처리
│   │   │   │   └── lock/           # LockManager
│   │   │   └── SeatLockApplication.java
│   │   └── resources/
│   │       ├── application.yml           # 로컬 설정
│   │       └── application-docker.yml    # Docker 설정
│   └── test/                         # 테스트 코드
├── docker-compose.yml                # 멀티 서버 구성
├── Dockerfile                        # Spring Boot 이미지
├── nginx.conf                        # Nginx 설정
└── README.md
```

---

## 📚 학습 포인트

### 1. 성능 측정의 중요성
- 이론적 예상 ≠ 실제 측정 결과
- 반드시 실측 후 의사결정

### 2. 트레이드오프 이해
- Redis: 확장성 ↑, 네트워크 오버헤드 ↑
- DB Lock: 성능 ↑, 단일 DB 의존성 ↑

### 3. 맥락에 맞는 기술 선택
- 프로젝트 특성 고려 (트랜잭션 속도, 인프라, 확장 계획)
- 맹목적인 기술 도입 지양
- 측정 기반 의사결정

---

