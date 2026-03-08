# IDStory 통합 로그인 시스템 - 개발 가이드

> **Spring Boot 3.2.3 + JDK 17 + MySQL 8** 기반의 통합 IAM / SSO 시스템

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [디렉터리 구조](#3-디렉터리-구조)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [초기 설정 및 실행](#5-초기-설정-및-실행)
6. [구현된 기능 목록](#6-구현된-기능-목록)
7. [URL 전체 목록](#7-url-전체-목록)
8. [테스트 계정](#8-테스트-계정)
9. [핵심 클래스 설명](#9-핵심-클래스-설명)
10. [CSS 컴포넌트 가이드](#10-css-컴포넌트-가이드)

---

## 1. 프로젝트 개요

IDStory는 기업 내부에서 사용하는 **통합 ID 관리 / SSO 시스템**입니다.
Spring Security 6 기반의 인증/인가 처리, OIDC Authorization Code Flow 기반 SSO 토큰 발급, 조직/역할/권한 관리, 통합 정책 관리, IP/MAC 접근 제어를 제공합니다.

### 주요 특징

- 한국어 기본, 다국어(i18n) 지원
- 비밀번호 암호화 알고리즘을 XML 설정으로 변경 가능 (SHA-256/512, HEX/BASE64)
- DB 기반 동적 트리 메뉴 + 역할별 접근 제어
- OIDC Authorization Code Flow 기반 SSO (RS256 JWT)
- 커스텀 `/sso/*` API (JSON 응답, form-urlencoded 요청)
- 7개 그룹의 통합 정책 관리 (비밀번호/로그인/계정/감사/시스템 등)
- IP CIDR / MAC 주소 기반 접근 제어
- 조직도/부서/역할/권한 전체 관리

---

## 2. 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | Spring Boot | 3.2.3 |
| 언어 | Java | 17 |
| 빌드 | Gradle | 8.x |
| 보안 | Spring Security | 6.x |
| 뷰 | Thymeleaf | 3.x |
| ORM | Spring Data JPA (Hibernate) | - |
| DB | MySQL | 8.0+ |
| JWT | JJWT | 0.12.6 |
| UI | Bootstrap 5 | CDN |
| 아이콘 | Font Awesome 6 | CDN |
| 기타 | Lombok, Bean Validation, @Async, @Scheduled | - |

---

## 3. 디렉터리 구조

```
idstory/
├── build.gradle
├── scripts/
│   ├── schema.sql                    ← DDL (전체 테이블 생성)
│   ├── data.sql                      ← DML (초기 데이터 + 정책 기본값)
│   ├── migrate_260308.sql            ← 마이그레이션 (2026-03-08)
│   └── migrate_260309.sql            ← 마이그레이션 (2026-03-09)
├── docs/
│   ├── PROJECT_GUIDE.md              ← 이 파일 (전체 가이드)
│   └── SSO_GUIDE.md                  ← SSO/OIDC 상세 가이드
├── logs/                             ← 로그 파일 (자동 생성)
└── src/main/
    ├── java/com/idstory/
    │   ├── IdstoryApplication.java         ← @SpringBootApplication, @EnableAsync, @EnableScheduling
    │   ├── common/
    │   │   ├── security/
    │   │   │   ├── SecurityConfig.java         ← Spring Security 설정
    │   │   │   ├── CustomPasswordEncoder.java  ← SHA-512/256 + HEX/BASE64
    │   │   │   ├── PasswordXmlProperties.java  ← password-config.xml 파싱
    │   │   │   └── AuthEventListener.java      ← 로그인 성공/실패 이벤트
    │   │   ├── util/
    │   │   │   └── OidGenerator.java           ← ids_+14자 영숫자 OID 생성
    │   │   └── web/
    │   │       ├── WebMvcConfig.java           ← MVC, i18n, Locale 설정
    │   │       └── GlobalControllerAdvice.java ← 전역 menuTree 주입
    │   ├── login/
    │   │   ├── controller/LoginController.java
    │   │   └── service/CustomUserDetailsService.java
    │   ├── password/
    │   │   ├── controller/PasswordResetController.java
    │   │   └── service/PasswordResetService.java
    │   ├── dashboard/
    │   │   ├── controller/MainController.java
    │   │   └── service/DashboardService.java
    │   ├── menu/
    │   │   ├── entity/Menu.java
    │   │   ├── repository/MenuRepository.java
    │   │   ├── service/MenuService.java
    │   │   └── controller/SystemMenuController.java
    │   ├── dept/
    │   │   ├── entity/Department.java
    │   │   ├── repository/DepartmentRepository.java
    │   │   ├── service/DepartmentService.java
    │   │   ├── controller/OrgController.java
    │   │   └── dto/DeptCreateDto.java, DeptUpdateDto.java
    │   ├── user/
    │   │   ├── entity/SysUser.java             ← ids_iam_user 엔티티
    │   │   ├── repository/SysUserRepository.java
    │   │   ├── service/SysUserService.java
    │   │   ├── controller/UserManagementController.java
    │   │   └── dto/UserCreateDto.java
    │   ├── admin/
    │   │   ├── entity/SysAdmin.java
    │   │   ├── repository/SysAdminRepository.java
    │   │   ├── service/AdminService.java
    │   │   └── controller/AdminManagementController.java
    │   ├── history/
    │   │   ├── entity/LoginHistory.java, UserAccountHistory.java
    │   │   ├── service/LoginHistoryService.java, UserAccountHistoryService.java
    │   │   └── controller/LoginHistoryController.java, UserHistoryController.java
    │   ├── policy/
    │   │   ├── entity/SystemPolicy.java, SystemPolicyHistory.java
    │   │   ├── service/SystemPolicyService.java, PasswordPolicyService.java
    │   │   ├── controller/PolicyManageController.java
    │   │   └── scheduler/AccountPolicyScheduler.java
    │   ├── accesscontrol/
    │   │   ├── entity/AccessControlRule.java, AccessControlHist.java
    │   │   ├── service/AccessControlService.java
    │   │   ├── filter/AccessControlFilter.java
    │   │   └── controller/AccessControlController.java, AccessDeniedPageController.java
    │   ├── roleuser/
    │   │   ├── entity/RoleUser.java, RoleSubject.java
    │   │   ├── service/RoleUserService.java, RoleSubjectService.java
    │   │   └── controller/RoleUserController.java
    │   └── sso/
    │       ├── entity/SsoClient.java, SsoKeyPair.java, SsoAuthCode.java
    │       │         SsoToken.java, SsoAccessLog.java
    │       ├── repository/ (위 엔티티별 Repository)
    │       ├── service/SsoKeyPairService.java, JwtTokenService.java
    │       │         SsoClientService.java, SsoAuthCodeService.java
    │       │         SsoTokenService.java, SsoAccessLogService.java
    │       ├── controller/OidcController.java    ← /oauth2/* (표준 OIDC)
    │       │             SsoManageController.java ← /sso/* (관리 페이지)
    │       │             SsoApiController.java    ← /sso/* (외부 API)
    │       └── dto/OidcTokenResponse.java, SsoClientCreateDto.java, SsoClientUpdateDto.java
    └── resources/
        ├── application.yml
        ├── config/password-config.xml
        ├── messages/messages.properties, messages_ko.properties, messages_en.properties
        ├── static/css/main.css, js/common.js
        └── templates/
            ├── include/ (layout.html, header.html, sidebar.html, footer.html)
            ├── login/   (login.html, password-reset-*.html)
            └── main/
                ├── dashboard.html
                ├── user/      (list.html, register.html, detail.html)
                ├── admin/     (list.html)
                ├── org/       (chart.html)
                ├── auth/      (client.html, role-user.html, access-control.html)
                ├── policy/    (manage-*.html × 8)
                ├── history/   (login.html, user-account.html)
                ├── sso/       (tokens.html, auth-codes.html, access-log.html)
                └── system/    (menu.html)
```

---

## 4. 데이터베이스 설계

### 테이블 목록 (ids_iam_* 네이밍 규칙)

| 테이블 | 설명 |
|--------|------|
| `ids_iam_user` | 사용자 계정 (status: ACTIVE/LOCKED/DORMANT/WITHDRAWN) |
| `ids_iam_dept` | 부서 (self-join 계층 구조, 소프트 삭제) |
| `ids_iam_admin` | 관리자 매핑 (ids_iam_user FK) |
| `ids_iam_login_hist` | 로그인/로그아웃/실패 이력 |
| `ids_iam_user_acct_hist` | 계정 변경 이력 |
| `ids_iam_policy` | 통합 정책 (policy_group + policy_key 복합 PK) |
| `ids_iam_policy_hist` | 정책 변경 이력 |
| `ids_iam_access_control` | IP/MAC 접근 제어 규칙 |
| `ids_iam_access_control_hist` | 접근 차단 이력 |
| `ids_iam_role_user` | 역할-사용자 직접 배정 |
| `ids_iam_role_subject` | 역할-대상 간접 배정 (DEPT/POSITION/GRADE) |
| `ids_iam_client` | 연계 클라이언트 (app_type: IAM/SSO/EAM/IEAM) |
| `ids_iam_sso_client` | OIDC SSO 클라이언트 설정 (ids_iam_client FK) |
| `ids_iam_sso_key_pair` | RSA-2048 서명 키 쌍 |
| `ids_iam_sso_auth_code` | Authorization Code 발행 이력 |
| `ids_iam_sso_token` | 발행 토큰 이력 (ACCESS/REFRESH/ID) |
| `ids_iam_sso_access_log` | SSO 접근 로그 |
| `menus` | 동적 메뉴 |
| `menu_roles` | 메뉴-역할 매핑 |
| `ids_iam_pwd_reset_token` | 비밀번호 초기화 토큰 |

### ids_iam_user 주요 컬럼

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `oid` | CHAR(18) PK | ids_+영숫자14자 |
| `user_id` | VARCHAR(50) UNIQUE | 로그인 계정 |
| `password` | VARCHAR(255) | 단방향 해시 (SHA-512 HEX 128자) |
| `name` | VARCHAR(100) | 표시 이름 |
| `use_yn` | CHAR(1) | Y/N |
| `status` | VARCHAR(20) | ACTIVE/LOCKED/DORMANT/WITHDRAWN |
| `locked_at` | DATETIME NULL | 계정 잠금 시각 (자동 해제 기준) |

### ids_iam_policy 주요 정책 그룹

| 그룹 | 설명 | 주요 키 |
|------|------|---------|
| `ADMIN_POLICY` | 관리자 정책 | ADMIN_MAX_LOGIN_FAIL, ADMIN_LOCK_AUTO_RELEASE_MINS |
| `USER_POLICY` | 사용자 정책 | MAX_LOGIN_FAIL_COUNT, USER_DORMANT_DAYS |
| `PASSWORD_POLICY` | 비밀번호 정책 | PWD_MIN_LEN, PWD_MAX_LEN, PWD_REQUIRE_* |
| `LOGIN_POLICY` | 로그인 정책 | SESSION_TIMEOUT 등 |
| `ACCOUNT_POLICY` | 계정 정책 | ACCT_LOCK_AUTO_RELEASE 등 |
| `AUDIT_POLICY` | 감사 정책 | - |
| `SYSTEM_POLICY` | 시스템 정책 | IP_ACCESS_CONTROL_ENABLED, MAC_ACCESS_CONTROL_ENABLED, SSO_ACCESS_TOKEN_EXTEND_SEC |

---

## 5. 초기 설정 및 실행

### 5.1 DB 초기화

```sql
-- MySQL에서 실행
source scripts/schema.sql;   -- 테이블 생성
source scripts/data.sql;     -- 초기 데이터 + 정책 기본값
```

> **기존 DB 업그레이드**: `scripts/migrate_260308.sql`, `migrate_260309.sql` 순서대로 실행

### 5.2 application.yml 주요 설정

```yaml
server:
  port: 9091

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/idstory_db?characterEncoding=UTF-8
    username: root
    password: (본인 설정)
  jpa:
    hibernate:
      ddl-auto: none

sso:
  issuer: http://localhost:9091   # JWT iss 클레임 / Discovery issuer
```

### 5.3 비밀번호 암호화 설정 (password-config.xml)

```xml
<password-config>
    <algorithm>SHA512</algorithm>   <!-- SHA256 | SHA512 -->
    <encoding>HEX</encoding>        <!-- HEX | BASE64 -->
    <password-salt-enabled>false</password-salt-enabled>
</password-config>
```

> MySQL 동일 해시 생성: `SELECT SHA2('1234', 512);` (HEX 기본값)

### 5.4 실행

```bash
./gradlew bootRun
# 접속: http://localhost:9091
```

---

## 6. 구현된 기능 목록

### 6.1 인증/인가

- **로그인**: Spring Security 폼 로그인 (POST /login), SHA-512 HEX 비밀번호 검증
- **비밀번호 초기화**: 토큰 UUID 발급 → URL 표시 → 변경 (운영 시 JavaMailSender로 교체)
- **로그아웃**: GET /logout → 세션 무효화 + 이력 기록
- **이벤트 리스너**: 로그인 성공/실패 자동 이력 기록 (`AuthEventListener`)

### 6.2 동적 메뉴 시스템

- DB 기반 트리 메뉴 (`menus` + `menu_roles`)
- 역할별 접근 제어: `menu_roles` 없음 = 전체, 있음 = 해당 역할만
- `GlobalControllerAdvice`로 모든 페이지에 menuTree 자동 주입
- 메뉴 잠금 (`locked=1`): 토글/삭제/역할수정 불가
- 관리 페이지: GET/POST `/system/menu`

### 6.3 사용자 관리

- 목록/조회/등록/수정/비활성화 (GET/POST `/user/**`)
- 상태 관리: ACTIVE/LOCKED/DORMANT/WITHDRAWN
- 자동 잠금/휴면 전환: `AccountPolicyScheduler` (@Scheduled 1분)
- 계정 변경 이력 자동 기록

### 6.4 조직 관리

- 부서 트리 구조 (self-join, 계층 무제한)
- 등록/수정/소프트 삭제/복원
- 조직도 페이지 (트리 + 상세 2열, 삭제된 부서 포함 토글)
- API: GET/POST `/org/api/depts/**`

### 6.5 관리자 관리

- 사용자 → 관리자 승격/해제
- 관리자 목록/상세/메모 수정
- URL: GET/POST `/admin/**`

### 6.6 역할-사용자 배정

- **직접 배정**: `ids_iam_role_user` (역할 ↔ 사용자 1:N)
- **간접 배정**: `ids_iam_role_subject` (부서/직위/직급/예외 대상)
- 유효 사용자 계산 (직접 + 간접 합산, 중복 제거)
- URL: GET `/auth/role-user`

### 6.7 통합 정책 관리

- 7개 그룹 56개 정책 키
- 변경 이력 자동 기록
- 실제 연동: 로그인 실패 횟수, 비밀번호 복잡도/길이, 휴면/잠금 자동 해제
- URL: GET/POST `/policy/manage/{tab}`
- 탭: admin / user / password / login / account / audit / system / history

### 6.8 IP/MAC 접근 제어

- IP: CIDR 범위 매칭 (IPv4/IPv6), X-Forwarded-For 지원
- MAC: X-Device-MAC 헤더, AA:BB:CC:DD:EE:FF 형식
- fail-open: 활성화되었어도 규칙 없으면 전체 허용 (잠금 방지)
- 차단 이력 조회
- 정책 연동: `SYSTEM_POLICY.IP_ACCESS_CONTROL_ENABLED` / `MAC_ACCESS_CONTROL_ENABLED`
- URL: GET `/auth/access-control`, GET `/access-denied`

### 6.9 클라이언트 관리

- 연계 시스템 등록/수정/사용여부 관리
- APP 유형: IAM / SSO / EAM / IEAM
- SSO 유형 선택 시 OIDC 설정 자동 활성화
- URL: GET `/auth/client`

### 6.10 SSO / OIDC

- **OIDC Authorization Code Flow** (RFC 6749 + OpenID Connect Core 1.0)
- RS256 (RSA-2048) JWT 발급 (Access / Refresh / ID Token)
- Client Secret: SHA-256 HEX 저장, 생성/재발급 시 1회만 표시
- Refresh Token Rotation (재발급 시 기존 취소)
- 자세한 내용: [SSO_GUIDE.md](SSO_GUIDE.md)

### 6.11 대시보드

- 통계 카드 6개 (사용자/관리자/클라이언트/오늘로그인/잠금/휴면)
- 월별 로그인 추이 (Chart.js)
- 보안 알림 4종 (장기미접속/로그인실패/잠금계정/관리자변경)
- URL: GET `/main/dashboard`

### 6.12 이력 관리

- 로그인 이력: GET `/history/login`
- 계정 변경 이력: GET `/history/user-account`

---

## 7. URL 전체 목록

### 공개 (인증 불필요)

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET/POST | `/login` | 로그인 |
| GET | `/logout` | 로그아웃 |
| GET/POST | `/password-reset` | 비밀번호 초기화 요청 |
| GET | `/password-reset/sent` | 초기화 링크 안내 |
| GET/POST | `/password-reset/confirm` | 비밀번호 변경 |
| GET | `/access-denied` | IP/MAC 차단 안내 |
| GET | `/.well-known/openid-configuration` | OIDC Discovery 문서 |
| GET | `/oauth2/jwks` | JWK Set (공개키) |
| POST | `/oauth2/token` | OIDC 토큰 발급 |
| GET | `/oauth2/userinfo` | OIDC 사용자 정보 |
| POST | `/sso/token` | SSO 토큰 발급 (JSON/Form) |
| POST | `/sso/userinfo` | SSO 사용자 정보 (Form) |
| POST | `/sso/logout` | SSO 로그아웃 (Form) |
| POST | `/sso/check` | SSO 로그인 체크 + 토큰 연장 (Form) |

### 인증 필요

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET | `/` 또는 `/main/dashboard` | 대시보드 |
| GET | `/sso/auth` | SSO Authorization Code 발급 |
| GET | `/oauth2/authorize` | OIDC Authorization 엔드포인트 |

### ADMIN 전용

| 메서드 | URL | 설명 |
|--------|-----|------|
| GET/POST | `/user/**` | 사용자 관리 |
| GET/POST | `/admin/**` | 관리자 관리 |
| GET | `/org/chart` | 조직도 관리 |
| GET/POST | `/org/api/**` | 부서 CRUD API |
| GET | `/auth/client` | 클라이언트 관리 |
| GET/POST | `/auth/client/{oid}/**` | 클라이언트 상세/SSO 설정 |
| GET | `/auth/role-user` | 역할-사용자 배정 |
| GET/POST | `/auth/role-user/**` | 역할-사용자 API |
| GET | `/auth/access-control` | IP/MAC 접근 제어 |
| GET/POST | `/auth/access-control/**` | 접근 제어 API |
| GET/POST | `/policy/manage/**` | 정책 관리 |
| GET | `/history/login` | 로그인 이력 |
| GET | `/history/user-account` | 계정 변경 이력 |
| GET | `/sso/tokens` | SSO 토큰 현황 |
| GET | `/sso/auth-codes` | Authorization Code 이력 |
| GET | `/sso/access-log` | SSO 접근 로그 |
| GET/POST | `/system/menu` | 메뉴 관리 |
| GET/POST | `/system/menu/{id}/**` | 메뉴 토글/삭제/역할 |

---

## 8. 테스트 계정

| 아이디 | 비밀번호 | 역할 | 상태 |
|--------|--------|------|------|
| admin | 1234 | ADMIN | ACTIVE |
| user1 | 1234 | USER | ACTIVE |
| user2 | 1234 | USER | ACTIVE |

---

## 9. 핵심 클래스 설명

### SecurityConfig.java

Spring Security 6 설정.
- CSRF: `/oauth2/token`, `/oauth2/userinfo`, `/sso/token`, `/sso/userinfo`, `/sso/logout`, `/sso/check` 제외
- `DaoAuthenticationProvider`: CustomUserDetailsService + CustomPasswordEncoder
- `AccessControlFilter`: UsernamePasswordAuthenticationFilter 앞에 삽입 (IP/MAC 차단)
- `FilterRegistrationBean.setEnabled(false)`: 이중 등록 방지

### CustomPasswordEncoder.java

`PasswordEncoder` 구현.
- `password-config.xml` 값 기반: SHA-256/512 + HEX/BASE64
- `encode()`: 단방향 해시
- `matches()`: 동일 방식 해시 후 비교

### SystemPolicyService.java

통합 정책 조회 서비스.
- `getInt(group, key, defaultValue)`: 정수 정책 조회
- `getBoolean(group, key, defaultValue)`: 불린 정책 조회
- `getString(group, key, defaultValue)`: 문자열 정책 조회
- `saveAll(group, Map<key,value>, performedBy)`: 일괄 저장 + 이력 기록

### SsoTokenService.java

SSO 토큰 관리.
- `issue()`: Authorization Code → ACCESS+REFRESH+ID 토큰 발급
- `issueFromRefresh()`: Refresh Token Rotation (기존 취소 + 새 토큰 3종 발급)
- `extendAccessToken()`: Access Token 검증 + 기존 취소 + 새 토큰 재발급 (로그인 체크)
- `validateAccessToken()`: Bearer 토큰 검증 → SysUser 반환
- `revokeAllByClientAndUser()`: 로그아웃 시 해당 client+user 전체 토큰 취소

### SsoApiController.java

외부 시스템 연동 전용 SSO API.
- `POST /sso/token`: authorization_code / refresh_token 그랜트
- `POST /sso/userinfo`: Bearer 토큰 검증 + 사용자 정보 반환
- `POST /sso/logout`: 전체 활성 토큰 일괄 취소
- `POST /sso/check`: 로그인 체크 + Access Token 연장 (SYSTEM_POLICY.SSO_ACCESS_TOKEN_EXTEND_SEC)

### OidGenerator.java

OID 생성 유틸.
- `generate()`: `ids_` + 영숫자 14자 (총 18자)
- `randomAlphanumeric(n)`: 무작위 영숫자 n자

---

## 10. CSS 컴포넌트 가이드

### 트리 사이드바

```css
.app-sidebar          /* 사이드바 컨테이너 (width: 245px) */
.tree-item            /* 하위 메뉴가 있는 항목 */
.tree-toggle          /* 클릭 가능한 토글 버튼 */
.tree-toggle.open     /* 펼쳐진 상태 */
.tree-arrow           /* 화살표 아이콘 (90도 회전 애니메이션) */
.tree-submenu         /* 서브메뉴 목록 (max-height 트랜지션) */
.tree-sublink         /* 서브메뉴 링크 (::before 점 표시) */
.tree-direct-link     /* 직접 링크 (하위 없는 최상위 메뉴) */
```

### 모달 공통

```css
.modal-label          /* 모달 내 레이블 */
.modal-input          /* 모달 내 input */
.modal-dialog-scrollable  /* 스크롤 가능 모달 */
```

### 역할/배지

```css
.role-badge           /* 기본 역할 배지 */
.role-badge.admin     /* ADMIN (파란색) */
.role-badge.user      /* USER (초록색) */
.role-badge.all       /* 전체 공개 (회색) */
```

### 기타

```css
.toggle-switch           /* CSS 토글 스위치 */
.toggle-switch.disabled  /* 잠금 상태 토글 */
.btn-delete              /* 삭제 버튼 (빨간색) */
.page-section            /* 페이지 섹션 컨테이너 */
.table-wrapper           /* 테이블 래퍼 */
```

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-02 | 프로젝트 초기 생성 (Spring Boot 3, Spring Security 6, 폼 로그인) |
| 2026-02 | 동적 트리 메뉴 + 역할별 접근 제어 + 메뉴 관리 UI |
| 2026-02 | 비밀번호 초기화 흐름 구현 |
| 2026-02 | 보안 이벤트 로깅 (AuthEventListener) |
| 2026-03 | ids_iam_user 기반 사용자 관리 시스템 구현 |
| 2026-03 | 조직 관리 (부서 트리, 조직도 페이지) |
| 2026-03 | 관리자 관리 (승격/해제, 상세보기) |
| 2026-03 | 역할-사용자 배정 (직접/간접/유효사용자 계산) |
| 2026-03 | 통합 정책 관리 시스템 (7개 그룹, AccountPolicyScheduler) |
| 2026-03 | IP/MAC 접근 제어 필터 + 관리 페이지 |
| 2026-03 | 대시보드 (통계카드, 보안알림 4종, Chart.js) |
| 2026-03-07 | SSO/OIDC 기능 구현 (OIDC Authorization Code Flow, RS256 JWT) |
| 2026-03-08 | 클라이언트 관리에 SSO 설정 통합, APP 유형(SSO/IAM/EAM/IEAM) |
| 2026-03-08 | SSO URI 등록 UI (AUTH_URI/AUTH_RESULT/REDIRECT_URI 통합 입력) |
| 2026-03-09 | `/sso/*` 커스텀 API 구현 (token/userinfo/logout/check) |
| 2026-03-09 | 로그인 체크 API (`/sso/check`) + Access Token 연장 정책 |
