# 🏗️ 1단계: 인프라 서비스 상세 가이드

이 문서는 MSA 프로젝트의 인프라 서비스인 **Discovery Service**와 **Config Service**에 대한 상세한 설명입니다.

---

## 📍 목차

1. [Discovery Service (Eureka Server)](#1-discovery-service-eureka-server)
2. [Config Service (Spring Cloud Config)](#2-config-service-spring-cloud-config)
3. [서비스 간 연동 흐름](#3-서비스-간-연동-흐름)

---

## 1. Discovery Service (Eureka Server)

### 1.1 개요

**Discovery Service**는 Netflix Eureka Server를 기반으로 한 **서비스 레지스트리**(Service Registry)입니다. 모든 마이크로서비스가 자신의 위치 정보를 등록하고, 다른 서비스들이 서로를 찾을 수 있게 해주는 핵심 인프라 서비스입니다.

### 1.2 역할

- **서비스 등록**: 각 마이크로서비스가 시작될 때 자신의 정보를 등록
- **서비스 발견**: 서비스 간 통신 시 서비스 위치 정보 제공
- **상태 모니터링**: 등록된 서비스들의 상태(UP, DOWN) 관리
- **로드 밸런싱 지원**: 동일한 서비스의 여러 인스턴스 관리

### 1.3 코드 분석

#### 1.3.1 메인 애플리케이션 클래스

```java
package com.example.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Service
 * 
 * 마이크로서비스들의 서비스 디스커버리를 담당하는 Eureka 서버입니다.
 * 모든 마이크로서비스들이 이 서버에 등록되고, 서로를 찾을 수 있게 해줍니다.
 * 
 * 접속 URL: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
```

**핵심 어노테이션**:
- `@SpringBootApplication`: Spring Boot 애플리케이션임을 나타냄
- `@EnableEurekaServer`: **이 애플리케이션을 Eureka Server로 활성화**

#### 1.3.2 설정 파일 분석

```yml
server:
  port: 8761

spring:
  application:
    name: discovery-service

eureka:
  instance:
    hostname: localhost
    prefer-ip-address: false
  client:
    # Eureka 서버 자체는 클라이언트로 등록되지 않도록 설정
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://localhost:8761/eureka/
  server:
    # 개발 환경에서 빠른 응답을 위한 설정
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
    
logging:
  level:
    com.netflix.eureka: OFF
    com.netflix.discovery: OFF
    org.springframework.cloud.netflix.eureka: DEBUG
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"

management:
  endpoints:
    web:
      exposure:
        include: health,info,env
  endpoint:
    health:
      show-details: always
```

**설정 항목 상세 설명**:

| 설정 항목 | 값 | 설명 |
|---------|-----|------|
| `server.port` | `8761` | Eureka Server가 실행되는 포트 |
| `eureka.client.register-with-eureka` | `false` | **중요**: Eureka Server 자체는 다른 Eureka Server에 등록하지 않음 (Standalone 모드) |
| `eureka.client.fetch-registry` | `false` | **중요**: Eureka Server는 다른 서버의 레지스트리를 가져오지 않음 |
| `eureka.server.enable-self-preservation` | `false` | Self-Preservation 모드 비활성화 (개발 환경용) |
| `eureka.server.eviction-interval-timer-in-ms` | `5000` | 5초마다 비정상 서비스를 제거 (개발 환경용 빠른 응답) |

**왜 `register-with-eureka: false`인가?**
- Eureka Server는 **서비스 레지스트리 자체**이므로, 자신을 등록할 필요가 없습니다.
- 만약 `true`로 설정하면, Eureka Server가 자신을 클라이언트로 등록하려고 시도하여 순환 참조 문제가 발생할 수 있습니다.

#### 1.3.3 의존성 분석

```groovy
plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
    
    // 추가적인 웹 인터페이스를 위한 의존성
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

**핵심 의존성**:
- `spring-cloud-starter-netflix-eureka-server`: Eureka Server 기능 제공
- `spring-boot-starter-web`: 웹 인터페이스 (대시보드) 제공

### 1.4 동작 원리

#### 1.4.1 서비스 등록 과정

```
1. Member Service 시작
   ↓
2. Eureka Client가 Discovery Service에 등록 요청
   - 서비스 이름: member-service
   - 인스턴스 ID: member-service:8081
   - 호스트: localhost
   - 포트: 8081
   - 상태: UP
   ↓
3. Discovery Service가 레지스트리에 저장
   ↓
4. 다른 서비스들이 이 정보를 조회 가능
```

#### 1.4.2 서비스 발견 과정

```
1. Order Service가 Member Service를 호출하려고 함
   ↓
2. OpenFeign이 "member-service" 이름으로 서비스 조회
   ↓
3. Discovery Service에서 "member-service" 인스턴스 목록 반환
   ↓
4. Load Balancer가 인스턴스 중 하나 선택
   ↓
5. 선택된 인스턴스로 요청 전송
```

### 1.5 Eureka Dashboard

Discovery Service가 실행되면 웹 대시보드를 통해 등록된 서비스를 확인할 수 있습니다:

- **URL**: http://localhost:8761
- **기능**:
  - 등록된 서비스 목록 확인
  - 각 서비스의 인스턴스 상태 확인
  - 서비스별 메타데이터 확인

### 1.6 실습

#### 1.6.1 Discovery Service 실행

```bash
./gradlew discovery-service:bootRun
```

#### 1.6.2 대시보드 확인

1. 브라우저에서 http://localhost:8761 접속
2. "Instances currently registered with Eureka" 섹션 확인
3. 다른 서비스들을 실행하면 목록에 나타나는지 확인

---

## 2. Config Service (Spring Cloud Config)

### 2.1 개요

**Config Service**는 Spring Cloud Config Server를 기반으로 한 **중앙 집중식 설정 관리 서비스**입니다. 모든 마이크로서비스의 설정 파일을 한 곳에서 관리하고, 환경별(dev, test, prod) 설정을 분리하여 제공합니다.

### 2.2 역할

- **중앙 설정 관리**: 모든 서비스의 설정을 한 곳에서 관리
- **환경별 설정 분리**: dev, test, prod 환경별로 다른 설정 제공
- **동적 설정 갱신**: 설정 변경 시 서비스에 반영 (Spring Cloud Bus 사용 시)
- **버전 관리**: Git을 통한 설정 파일 버전 관리 (선택사항)

### 2.3 코드 분석

#### 2.3.1 메인 애플리케이션 클래스

```java
package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Configuration Service
 * 
 * 중앙화된 설정 관리를 담당하는 Spring Cloud Config 서버입니다.
 * 모든 마이크로서비스들의 설정을 중앙에서 관리하고 배포합니다.
 * 
 * 접속 URL: http://localhost:8888
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
```

**핵심 어노테이션**:
- `@EnableConfigServer`: **이 애플리케이션을 Config Server로 활성화**

#### 2.3.2 설정 파일 분석

```yml
server:
  port: 8888

spring:
  application:
    name: config-service
  cloud:
    config:
      server:
        # 로컬 파일시스템에서 설정 파일을 읽어옴
        native:
          search-locations: classpath:/config-repo/
        # Git 레포지토리 사용 시 (선택사항)
        # git:
        #   uri: https://github.com/your-username/config-repo
        #   clone-on-start: true
  profiles:
    active: native

logging:
  level:
    org.springframework.cloud.config: DEBUG
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"

management:
  endpoints:
    web:
      exposure:
        include: health,info,env,configprops
  endpoint:
    health:
      show-details: always
```

**설정 항목 상세 설명**:

| 설정 항목 | 값 | 설명 |
|---------|-----|------|
| `server.port` | `8888` | Config Server가 실행되는 포트 |
| `spring.profiles.active` | `native` | **Native 프로파일**: 로컬 파일시스템에서 설정 파일 읽기 |
| `spring.cloud.config.server.native.search-locations` | `classpath:/config-repo/` | 설정 파일이 위치한 경로 |

**Native vs Git 프로파일**:
- **Native**: 로컬 파일시스템에서 설정 파일 읽기 (개발 환경에 적합)
- **Git**: Git 레포지토리에서 설정 파일 읽기 (프로덕션 환경에 적합)

#### 2.3.3 설정 파일 구조

Config Service는 `config-repo` 디렉토리에서 설정 파일을 읽습니다:

```
config-service/src/main/resources/config-repo/
├── application.yml          # 모든 서비스에 공통 적용되는 설정
├── gateway-service.yml      # Gateway Service 전용 설정
├── member-service.yml        # Member Service 전용 설정
├── order-service.yml        # Order Service 전용 설정
└── auth-service.yml         # Auth Service 전용 설정
```

**설정 파일 명명 규칙**:
- `{application-name}.yml`: 특정 서비스 전용 설정
- `application.yml`: 모든 서비스에 공통 적용되는 설정

#### 2.3.4 공통 설정 파일

```yml
# 모든 서비스에 공통으로 적용되는 설정

eureka:
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 30
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,env
  endpoint:
    health:
      show-details: always

logging:
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}"
```

이 파일의 설정은 **모든 서비스에 공통으로 적용**됩니다. 각 서비스는 자신의 전용 설정 파일과 이 공통 설정을 병합하여 사용합니다.

### 2.4 클라이언트 설정 (bootstrap.yml)

각 서비스는 `bootstrap.yml` 파일을 통해 Config Service에 연결합니다:

```yml
spring:
  application:
    name: gateway-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        initial-interval: 2000
        max-attempts: 6
        max-interval: 10000
```

**설정 항목 상세 설명**:

| 설정 항목 | 값 | 설명 |
|---------|-----|------|
| `spring.application.name` | `gateway-service` | **중요**: Config Server에서 이 이름으로 설정 파일을 찾음 (`gateway-service.yml`) |
| `spring.cloud.config.uri` | `http://localhost:8888` | Config Server의 URL |
| `spring.cloud.config.fail-fast` | `true` | Config Server 연결 실패 시 애플리케이션 시작 중단 |
| `spring.cloud.config.retry.*` | - | Config Server 연결 재시도 설정 |

**bootstrap.yml vs application.yml**:
- `bootstrap.yml`: **애플리케이션 시작 전**에 로드됨 (Config Server 연결용)
- `application.yml`: 애플리케이션 시작 후 로드됨 (로컬 설정용)

### 2.5 설정 로드 순서

```
1. 애플리케이션 시작
   ↓
2. bootstrap.yml 로드
   - spring.application.name 확인
   - spring.cloud.config.uri 확인
   ↓
3. Config Server에 연결
   - GET http://localhost:8888/{application-name}/default
   ↓
4. 설정 파일 로드
   - application.yml (공통 설정)
   - {application-name}.yml (서비스 전용 설정)
   ↓
5. 설정 병합 및 적용
   ↓
6. 애플리케이션 시작 완료
```

**설정 우선순위** (높은 순서대로):
1. 서비스 전용 설정 (`{application-name}.yml`)
2. 공통 설정 (`application.yml`)
3. 로컬 설정 (`application.yml` - 서비스 내부)

### 2.6 Config Server API

Config Server는 REST API를 통해 설정을 제공합니다:

#### 2.6.1 기본 엔드포인트

```
GET http://localhost:8888/{application-name}/{profile}
```

**예시**:
- `GET http://localhost:8888/gateway-service/default`
- `GET http://localhost:8888/member-service/default`

#### 2.6.2 응답 형식

```json
{
  "name": "gateway-service",
  "profiles": ["default"],
  "label": null,
  "version": null,
  "state": null,
  "propertySources": [
    {
      "name": "classpath:/config-repo/application.yml",
      "source": {
        "eureka.client.service-url.defaultZone": "http://localhost:8761/eureka/",
        ...
      }
    },
    {
      "name": "classpath:/config-repo/gateway-service.yml",
      "source": {
        "server.port": 8080,
        ...
      }
    }
  ]
}
```

### 2.7 실습

#### 2.7.1 Config Service 실행

```bash
./gradlew config-service:bootRun
```

#### 2.7.2 설정 확인

1. 브라우저에서 http://localhost:8888/gateway-service/default 접속
2. JSON 형식으로 설정이 반환되는지 확인
3. 다른 서비스의 설정도 확인:
   - http://localhost:8888/member-service/default
   - http://localhost:8888/order-service/default
   - http://localhost:8888/auth-service/default

#### 2.7.3 설정 파일 수정 테스트

1. `config-service/src/main/resources/config-repo/gateway-service.yml` 파일 수정
2. Config Service 재시작
3. http://localhost:8888/gateway-service/default 접속하여 변경사항 확인

---

## 3. 서비스 간 연동 흐름

### 3.1 전체 시작 순서

```
1. Discovery Service 시작 (8761)
   ↓
2. Config Service 시작 (8888)
   ↓
3. Gateway Service 시작 (8080)
   - bootstrap.yml → Config Service 연결
   - Config Service에서 설정 로드
   - Eureka Client → Discovery Service에 등록
   ↓
4. Member Service 시작 (8081)
   - bootstrap.yml → Config Service 연결
   - Config Service에서 설정 로드
   - Eureka Client → Discovery Service에 등록
   ↓
5. Order Service 시작 (8082)
   - bootstrap.yml → Config Service 연결
   - Config Service에서 설정 로드
   - Eureka Client → Discovery Service에 등록
   ↓
6. Auth Service 시작 (8083)
   - bootstrap.yml → Config Service 연결
   - Config Service에서 설정 로드
   - Eureka Client → Discovery Service에 등록
```

### 3.2 설정 로드 흐름

```
각 서비스 시작 시:

1. bootstrap.yml 읽기
   - spring.application.name: gateway-service
   - spring.cloud.config.uri: http://localhost:8888
   ↓
2. Config Service에 요청
   GET http://localhost:8888/gateway-service/default
   ↓
3. Config Service가 설정 파일 반환
   - application.yml (공통)
   - gateway-service.yml (전용)
   ↓
4. 설정 병합 및 적용
   ↓
5. Eureka Client가 Discovery Service에 등록
   POST http://localhost:8761/eureka/apps/gateway-service
   ↓
6. 애플리케이션 시작 완료
```

### 3.3 서비스 발견 흐름

```
Order Service가 Member Service를 호출할 때:

1. OpenFeign이 "member-service" 이름으로 서비스 조회
   ↓
2. Eureka Client가 Discovery Service에 요청
   GET http://localhost:8761/eureka/apps/member-service
   ↓
3. Discovery Service가 인스턴스 목록 반환
   [
     {
       "instanceId": "member-service:8081",
       "hostName": "localhost",
       "port": 8081,
       "status": "UP"
     }
   ]
   ↓
4. Load Balancer가 인스턴스 선택
   ↓
5. 선택된 인스턴스로 요청 전송
   GET http://localhost:8081/members/1
```

---

## 4. 핵심 개념 정리

### 4.1 Discovery Service

| 개념 | 설명 |
|------|------|
| **Service Registry** | 서비스들의 위치 정보를 저장하는 저장소 |
| **Service Discovery** | 서비스를 찾는 메커니즘 |
| **Eureka Client** | 서비스를 등록하고 조회하는 클라이언트 |
| **Eureka Server** | 서비스 레지스트리를 관리하는 서버 |

### 4.2 Config Service

| 개념 | 설명 |
|------|------|
| **Centralized Configuration** | 중앙 집중식 설정 관리 |
| **Native Profile** | 로컬 파일시스템에서 설정 읽기 |
| **Git Profile** | Git 레포지토리에서 설정 읽기 |
| **bootstrap.yml** | Config Server 연결 설정 (애플리케이션 시작 전 로드) |
| **application.yml** | 로컬 설정 (애플리케이션 시작 후 로드) |

---

## 5. 실습 체크리스트

### Discovery Service
- [ ] Discovery Service 실행
- [ ] http://localhost:8761 접속하여 대시보드 확인
- [ ] 다른 서비스 실행 후 등록되는지 확인
- [ ] 서비스 상태(UP/DOWN) 확인

### Config Service
- [ ] Config Service 실행
- [ ] http://localhost:8888/{service-name}/default 접속하여 설정 확인
- [ ] 설정 파일 구조 이해
- [ ] bootstrap.yml의 역할 이해
- [ ] 설정 우선순위 이해

### 통합 테스트
- [ ] 모든 서비스를 순서대로 시작
- [ ] 각 서비스가 Config Service에서 설정을 로드하는지 확인
- [ ] 각 서비스가 Discovery Service에 등록되는지 확인
- [ ] 서비스 간 통신이 정상적으로 동작하는지 확인

---

## 6. 다음 단계

인프라 서비스를 이해했다면, 다음 단계로 진행하세요:

1. **Auth Service**: JWT 기반 인증/인가 구현 방법 학습
2. **Member Service**: Spring Data JPA를 활용한 CRUD 서비스 학습
3. **Order Service**: OpenFeign을 통한 서비스 간 통신 학습
4. **Gateway Service**: API Gateway의 라우팅 및 인증 필터 학습
