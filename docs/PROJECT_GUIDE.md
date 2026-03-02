# IDStory 통합 로그인 시스템 - 개발 가이드

> **Spring Boot 3.2.3 + JDK 17 + MySQL 8** 기반의 통합 로그인 / 메뉴 관리 시스템

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [디렉터리 구조](#3-디렉터리-구조)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [초기 설정 및 실행](#5-초기-설정-및-실행)
6. [주요 기능 구현](#6-주요-기능-구현)
7. [테스트 계정](#7-테스트-계정)
8. [API / URL 목록](#8-api--url-목록)
9. [핵심 클래스 설명](#9-핵심-클래스-설명)
10. [CSS 컴포넌트 가이드](#10-css-컴포넌트-가이드)

---

## 1. 프로젝트 개요

IDStory는 기업 내부에서 사용하는 **통합 로그인(SSO) 시스템의 기반 프레임워크**입니다.
Spring Security 6 기반의 인증/인가 처리와 DB 기반 동적 메뉴 관리, 역할별 접근 제어를 제공합니다.

### 주요 특징

- 한국어 기본, 다국어(i18n) 지원
- 비밀번호 암호화 알고리즘을 XML 설정으로 변경 가능
- DB에서 직접 관리하는 동적 트리 메뉴
- 역할(ROLE)별 메뉴 접근 제어
- 로그인/로그아웃/비밀번호 초기화 등 모든 보안 이벤트 로깅

---

## 2. 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | Spring Boot | 3.2.3 |
| 언어 | Java | 17 |
| 빌드 | Gradle | 8.x |
| 보안 | Spring Security | 6.x |
| 뷰 | Thymeleaf | 3.x |
| 보안 통합 | thymeleaf-extras-springsecurity6 | - |
| ORM | Spring Data JPA (Hibernate) | - |
| DB | MySQL | 8.0+ |
| UI | Bootstrap 5 | CDN |
| 아이콘 | Font Awesome 6 | CDN |
| 기타 | Lombok, Bean Validation | - |

---

## 3. 디렉터리 구조

```
idstory/
├── build.gradle
├── settings.gradle
├── scripts/                          ← DB 스크립트 (수동 실행)
│   ├── schema.sql                    ← DDL (테이블 생성)
│   └── data.sql                      ← DML (초기 데이터)
├── docs/
│   └── PROJECT_GUIDE.md              ← 이 파일
├── logs/                             ← 로그 파일 (자동 생성)
│   └── idstory.log
└── src/main/
    ├── java/com/idstory/auth/
    │   ├── IdstoryApplication.java
    │   ├── advice/
    │   │   └── GlobalControllerAdvice.java     ← 전역 menuTree 주입
    │   ├── config/
    │   │   ├── SecurityConfig.java             ← Spring Security 설정
    │   │   ├── WebMvcConfig.java               ← MVC, 다국어 설정
    │   │   ├── PasswordXmlProperties.java      ← XML 암호화 설정 파싱
    │   │   └── CustomPasswordEncoder.java      ← SHA-512/256 + BASE64/HEX
    │   ├── controller/
    │   │   ├── LoginController.java            ← /login
    │   │   ├── MainController.java             ← /main/**
    │   │   ├── PasswordResetController.java    ← /password-reset
    │   │   └── SystemMenuController.java       ← /system/menu (ADMIN)
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Menu.java                       ← menus + menu_roles 엔티티
    │   │   └── PasswordResetToken.java
    │   ├── listener/
    │   │   └── AuthEventListener.java          ← 로그인 성공/실패 이벤트 로깅
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   ├── MenuRepository.java             ← JOIN FETCH 쿼리 포함
    │   │   └── PasswordResetTokenRepository.java
    │   ├── service/
    │   │   ├── CustomUserDetailsService.java
    │   │   ├── MenuService.java                ← 트리 빌드 + 역할 필터링
    │   │   └── PasswordResetService.java
    │   └── util/
    └── resources/
        ├── application.yml                     ← 서버 설정 (포트 9091)
        ├── config/
        │   └── password-config.xml             ← 암호화 알고리즘/인코딩 설정
        ├── messages/
        │   ├── messages.properties             ← 기본(fallback)
        │   ├── messages_ko.properties          ← 한국어
        │   └── messages_en.properties          ← 영어
        ├── static/
        │   ├── css/
        │   │   └── main.css                    ← 전체 커스텀 스타일
        │   └── js/
        │       └── common.js                   ← 트리 메뉴 토글 + 액티브 감지
        └── templates/
            ├── include/
            │   ├── layout.html                 ← 공통 레이아웃 프래그먼트
            │   ├── header.html                 ← 프로필 드롭다운 포함
            │   ├── sidebar.html                ← 동적 트리 메뉴
            │   └── footer.html
            ├── login/
            │   ├── login.html
            │   ├── password-reset-request.html
            │   ├── password-reset-sent.html
            │   └── password-reset-form.html
            ├── main/
            │   ├── dashboard.html
            │   └── system/
            │       └── menu.html               ← 메뉴 관리 (ADMIN)
            └── error/
```

---

## 4. 데이터베이스 설계

### 4.1 users 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 사용자 PK |
| username | VARCHAR(50) UNIQUE | 로그인 아이디 |
| password | VARCHAR(255) | SHA-512 + BASE64 암호화 비밀번호 |
| email | VARCHAR(100) UNIQUE | 이메일 |
| name | VARCHAR(100) | 표시 이름 |
| role | VARCHAR(20) DEFAULT 'USER' | 권한 (USER \| ADMIN) |
| enabled | TINYINT(1) DEFAULT 1 | 계정 활성화 여부 |
| created_at / updated_at | DATETIME | 생성/수정 일시 |

### 4.2 menus 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| menu_id | BIGINT PK AUTO_INCREMENT | 메뉴 PK |
| parent_id | BIGINT NULL FK→menu_id | 상위 메뉴 ID (NULL = 최상위) |
| menu_name | VARCHAR(100) | 메뉴명 |
| icon | VARCHAR(100) NULL | Font Awesome 클래스 |
| url | VARCHAR(255) NULL | 링크 URL (NULL = 폴더형) |
| sort_order | INT DEFAULT 0 | 정렬 순서 |
| enabled | TINYINT(1) DEFAULT 1 | 활성화 여부 |
| locked | TINYINT(1) DEFAULT 0 | 잠금 여부 (1 = 관리 불가, 시스템 필수) |

> `parent_id` FK는 `ON DELETE CASCADE` → 상위 삭제 시 하위도 자동 삭제

### 4.3 menu_roles 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| menu_id | BIGINT PK, FK→menus | 메뉴 FK |
| role | VARCHAR(20) PK | 권한 (USER \| ADMIN) |

> **역할 규칙**
> - `menu_roles`에 행이 없으면 → **모든 인증 사용자**에게 표시
> - 행이 있으면 → **해당 역할을 가진 사용자**에게만 표시

### 4.4 password_reset_tokens 테이블

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 토큰 PK |
| token | VARCHAR(36) UNIQUE | UUID 토큰 |
| username | VARCHAR(50) | 대상 사용자 아이디 |
| expiry_date | DATETIME | 만료 일시 (24시간) |
| used | TINYINT(1) DEFAULT 0 | 사용 여부 |

### 4.5 메뉴 초기 데이터 구조

```
대시보드             (locked=1, 역할 없음 = 전체 공개)
사용자 관리          (ADMIN)
  ├── 사용자 목록
  ├── 사용자 등록
  └── 사용자 그룹
조직 관리            (ADMIN)
  ├── 조직도 / 부서 관리 / 직책 관리
권한 관리            (ADMIN)
  ├── 역할 관리 / 권한 설정 / 접근 제어
인증 관리            (ADMIN)
  ├── 인증 정책 / MFA 설정 / 세션 관리
정책 관리            (ADMIN)
  ├── 보안 정책 / 비밀번호 정책 / 접속 정책
시스템 연계          (ADMIN)
  ├── 연계 시스템 목록 / API 관리 / SSO 설정
감사 / 이력 관리     (ADMIN)
  ├── 로그인 이력 / 접근 이력 / 변경 이력
통계 / 리포트        (ADMIN)
  ├── 사용자 통계 / 접근 통계 / 보고서 생성
시스템 설정          (ADMIN)
  ├── 기본 설정 / 알림 설정 / 백업 관리
  └── 메뉴 관리     → /system/menu
```

---

## 5. 초기 설정 및 실행

### 5.1 필수 환경

- Java 17+
- MySQL 8.0+
- Gradle 8.x (또는 내장 `./gradlew` 사용)

### 5.2 DB 초기화

```sql
-- 1단계: 테이블 생성
source scripts/schema.sql;

-- 2단계: 초기 데이터 입력
source scripts/data.sql;
```

> 재실행 시 `data.sql`은 기존 데이터를 `DELETE` 후 재삽입합니다.

### 5.3 application.yml 주요 설정

```yaml
server:
  port: 9091

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/idstory_db?characterEncoding=UTF-8
    username: root
    password: (본인 설정)
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none     # 스크립트 직접 관리
    show-sql: false

logging:
  file:
    name: logs/idstory.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      total-size-cap: 1GB
```

### 5.4 비밀번호 암호화 설정 (password-config.xml)

```xml
<password-config>
    <algorithm>SHA512</algorithm>   <!-- SHA256 | SHA512 -->
    <encoding>BASE64</encoding>     <!-- HEX | BASE64 -->
    <salt-enabled>false</salt-enabled>
</password-config>
```

> MySQL에서 동일한 해시 생성: `SELECT TO_BASE64(UNHEX(SHA2('1234', 512)));`

### 5.5 빌드 및 실행

```bash
# Gradle 빌드
./gradlew build

# 실행
./gradlew bootRun
# 또는
java -jar build/libs/idstory-0.0.1-SNAPSHOT.jar
```

접속: `http://localhost:9091`

---

## 6. 주요 기능 구현

### 6.1 Spring Security 인증

- `SecurityConfig` → `SecurityFilterChain` Bean 설정
- `DaoAuthenticationProvider` + `CustomUserDetailsService`
- 로그인 URL: `POST /login`, 로그아웃: `POST /logout`
- `@EnableMethodSecurity` → `@PreAuthorize("hasRole('ADMIN')")` 활성화
- 세션 기반 인증 (기본값)

### 6.2 비밀번호 암호화

```java
// CustomPasswordEncoder.encode()
MessageDigest md = MessageDigest.getInstance("SHA-512");
byte[] hash = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
return Base64.getEncoder().encodeToString(hash);
```

알고리즘/인코딩은 `password-config.xml`에서 런타임 변경 가능.

### 6.3 동적 DB 메뉴 + 역할 필터링

```
DB (menus + menu_roles)
    ↓
MenuService.getMenuTreeByRoles(Set<String> userRoles)
    → 1) JOIN FETCH로 N+1 방지
    → 2) 역할 기반 필터링 (empty = 전체, non-empty = 교집합)
    → 3) buildTree() → parent_id 기반 트리 구성
    ↓
GlobalControllerAdvice.menuTree() [@ModelAttribute("menuTree")]
    → Authentication에서 역할 추출 (ROLE_ 접두사 제거)
    ↓
sidebar.html → th:each="menu : ${menuTree}"
```

### 6.4 트리 메뉴 케이스 처리 (sidebar.html)

| 조건 | 렌더링 |
|------|--------|
| 하위 메뉴 있음 | 토글 버튼 + 서브메뉴 목록 |
| 하위 없음 + 실제 URL | 직접 링크 (`<a>`) |
| 하위 없음 + URL 없음 또는 `#` | 렌더링 안 함 |

### 6.5 비밀번호 초기화 흐름

```
/password-reset            → 아이디 + 이메일 입력
    ↓ POST
PasswordResetService.createResetToken()
    → UUID 생성, 24시간 유효, DB 저장
    ↓
/password-reset/sent        → 토큰 링크 화면에 표시 (데모용)
    ↓ 링크 클릭
/password-reset/confirm?token=...
    → 토큰 유효성 검증 (미사용 + 미만료)
    ↓ 새 비밀번호 입력
    → resetPassword() → 비밀번호 업데이트, 토큰 used=1
    ↓
인증 상태에 따라 리다이렉트:
    - 미인증 → /login
    - 인증됨 → /main/dashboard
```

### 6.6 보안 이벤트 로깅

| 이벤트 | 로그 위치 | 레벨 |
|--------|----------|------|
| 로그인 성공 | `AuthEventListener` | INFO |
| 로그인 실패 | `AuthEventListener` | WARN |
| 로그아웃 | `SecurityConfig.LogoutSuccessHandler` | INFO |
| 비밀번호 초기화 각 단계 | `PasswordResetController` | INFO/WARN |
| 메뉴 관리 접근/수정 | `SystemMenuController` | INFO |
| 대시보드 접근 | `MainController` | INFO |

로그 파일: `logs/idstory.log` (10MB 롤링, 30일 보관)

### 6.7 메뉴 잠금(locked) 정책

| 조건 | 동작 |
|------|------|
| `locked=1` 메뉴 토글 시도 | `IllegalStateException` 발생 |
| `locked=1` 메뉴 삭제 시도 | `IllegalStateException` 발생 |
| `locked=1` 메뉴 역할 수정 시도 | `IllegalStateException` 발생 |
| UI에서 잠금 메뉴 | 토글 비활성화, 삭제 버튼 숨김, "잠금" 텍스트 표시 |

---

## 7. 테스트 계정

| 아이디 | 비밀번호 | 역할 | 상태 |
|--------|--------|------|------|
| admin | 1234 | ADMIN | 활성 |
| user1 | 1234 | USER | 활성 |
| user2 | 1234 | USER | 활성 |
| disabled_user | 1234 | USER | 비활성 |

> ADMIN 로그인 시: 모든 메뉴 표시
> USER 로그인 시: 대시보드만 표시 (다른 메뉴는 ADMIN 전용)

---

## 8. API / URL 목록

### 공개 (인증 불필요)

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET/POST | `/login` | 로그인 |
| GET/POST | `/password-reset` | 비밀번호 초기화 요청 |
| GET | `/password-reset/sent` | 초기화 링크 안내 |
| GET/POST | `/password-reset/confirm` | 비밀번호 변경 |

### 인증 필요

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/main/dashboard` | 대시보드 |
| POST | `/logout` | 로그아웃 |

### ADMIN 전용

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/system/menu` | 메뉴 관리 목록 |
| POST | `/system/menu` | 메뉴 추가 |
| POST | `/system/menu/{id}/toggle` | 활성화 토글 |
| POST | `/system/menu/{id}/delete` | 메뉴 삭제 |
| POST | `/system/menu/{id}/roles` | 역할 수정 |

---

## 9. 핵심 클래스 설명

### SecurityConfig.java
Spring Security 6 설정 클래스.
- `SecurityFilterChain`: 인증/인가 필터 체인 설정
- `DaoAuthenticationProvider`: 커스텀 UserDetailsService + PasswordEncoder 연결
- `LogoutSuccessHandler`: 로그아웃 후 로그 기록 → `/login?logout=true` 리다이렉트

### CustomPasswordEncoder.java
`PasswordEncoder` 인터페이스 구현.
- `encode()`: 단방향 해시 (SHA-512 or SHA-256, BASE64 or HEX)
- `matches()`: 입력값을 동일 방식으로 해시 후 비교

### Menu.java (Entity)
```java
@Entity @Table(name = "menus")
public class Menu {
    // DB 컬럼 필드
    private Long menuId, parentId;
    private String menuName, icon, url;
    private int sortOrder;
    private boolean enabled, locked;

    // 역할 목록 (menu_roles 테이블)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "menu_roles", joinColumns = @JoinColumn(name = "menu_id"))
    private Set<String> roles = new HashSet<>();

    // 트리 구성용 (DB 저장 안 됨)
    @Transient
    private List<Menu> children = new ArrayList<>();
}
```

### MenuService.java
```java
// 역할 기반 메뉴 필터링 + 트리 구성
public List<Menu> getMenuTreeByRoles(Set<String> userRoles) {
    List<Menu> all = menuRepository.findEnabledWithRoles();
    List<Menu> filtered = all.stream()
        .filter(m -> m.getRoles().isEmpty()      // 역할 없음 = 전체 공개
                  || m.getRoles().stream().anyMatch(userRoles::contains))
        .collect(Collectors.toList());
    return buildTree(filtered);
}
```

### GlobalControllerAdvice.java
모든 컨트롤러 실행 전 `menuTree`를 모델에 자동 주입.
```java
@ModelAttribute("menuTree")
public List<Menu> menuTree(Authentication auth) {
    Set<String> roles = auth.getAuthorities().stream()
        .map(a -> a.getAuthority().replace("ROLE_", ""))
        .collect(Collectors.toSet());
    return menuService.getMenuTreeByRoles(roles);
}
```

### AuthEventListener.java
Spring Security 이벤트 기반 로그 기록.
```java
@EventListener
public void onSuccess(AuthenticationSuccessEvent e) {
    // username, authorities, IP 로깅
}
@EventListener
public void onFailure(AbstractAuthenticationFailureEvent e) {
    // username, 실패 유형, IP 로깅
}
```

---

## 10. CSS 컴포넌트 가이드

### 트리 사이드바

```css
.app-sidebar          /* 사이드바 컨테이너 (width: 245px) */
.tree-item            /* 하위 메뉴가 있는 항목 */
.tree-toggle          /* 클릭 가능한 토글 버튼 */
.tree-toggle.open     /* 펼쳐진 상태 (JS에서 토글) */
.tree-arrow           /* 화살표 아이콘 (90도 회전 애니메이션) */
.tree-submenu         /* 서브메뉴 목록 (max-height 트랜지션) */
.tree-sublink         /* 서브메뉴 링크 (::before 점 표시) */
.tree-direct-link     /* 직접 링크 (하위 없는 최상위 메뉴) */
```

### 헤더 프로필 드롭다운

```css
.user-profile-btn     /* 우상단 프로필 버튼 */
.user-dropdown        /* 드롭다운 패널 */
.profile-header       /* 드롭다운 상단 (아바타 + 이름 + 역할) */
.profile-avatar-lg    /* 드롭다운 내 큰 아바타 */
.logout-item          /* 로그아웃 항목 (hover 시 빨간색) */
```

### 메뉴 관리 페이지

```css
.menu-table           /* 메뉴 목록 테이블 */
.row-parent           /* 최상위 메뉴 행 */
.row-child            /* 하위 메뉴 행 (들여쓰기) */
.badge-top            /* 상위 배지 */
.badge-sub            /* 하위 배지 */
.toggle-switch        /* CSS 토글 스위치 */
.toggle-switch.disabled /* 잠금 메뉴 토글 (클릭 불가) */
.btn-delete           /* 삭제 버튼 (빨간색) */
.btn-role-edit        /* 역할 편집 버튼 */
```

### 역할 배지

```css
.role-badge           /* 기본 역할 배지 */
.role-badge.admin     /* ADMIN (파란색) */
.role-badge.user      /* USER (초록색) */
.role-badge.all       /* 전체 공개 (회색) */
```

### 메뉴 범례

```css
.menu-legend          /* 페이지 상단 범례 컨테이너 */
.legend-item          /* 개별 범례 항목 */
.legend-sep           /* 구분선 */
```

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-02 | 프로젝트 초기 생성 (Spring Boot 3, Gradle, Spring Security 6) |
| 2026-02 | 로그인 페이지 UI (흰 배경 + 파란 테마, 한국어 기본) |
| 2026-02 | 정적 파일 구조 정리 (static/css, static/js, templates 정리) |
| 2026-02 | 프로필 드롭다운 (비밀번호 변경 + 로그아웃) |
| 2026-02 | 비밀번호 초기화 리다이렉트 수정 (인증 상태에 따라 분기) |
| 2026-02 | 보안 이벤트 로깅 추가 (로그인/로그아웃/비밀번호 초기화) |
| 2026-02 | DB 기반 동적 트리 메뉴 구현 |
| 2026-02 | 역할별 메뉴 접근 제어 + 잠금 메뉴 + 메뉴 관리 UI |
