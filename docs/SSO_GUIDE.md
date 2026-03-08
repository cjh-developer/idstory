# IDStory SSO / OIDC 가이드

> IDStory 통합 로그인 시스템의 OIDC 기반 SSO 기능 상세 가이드

---

## 목차

1. [개요](#1-개요)
2. [데이터베이스 구조](#2-데이터베이스-구조)
3. [패키지 구조](#3-패키지-구조)
4. [클라이언트 관리](#4-클라이언트-관리)
5. [OIDC 흐름 (표준 `/oauth2/*`)](#5-oidc-흐름-표준-oauth2)
6. [커스텀 SSO API (`/sso/*`)](#6-커스텀-sso-api-sso)
7. [JWT 토큰 구조](#7-jwt-토큰-구조)
8. [Client Secret 관리 정책](#8-client-secret-관리-정책)
9. [관리 페이지 URL](#9-관리-페이지-url)
10. [설정 파일](#10-설정-파일)

---

## 1. 개요

IDStory SSO는 **OIDC Authorization Code Flow** 를 구현한 경량 SSO 서버입니다.

- RFC 6749 (OAuth 2.0) + OpenID Connect Core 1.0 준수
- RS256 (RSA-2048) 서명 JWT 발급
- 세션 기반 인증 (Spring Security) + OIDC 토큰 발급 병행
- 표준 OIDC 엔드포인트(`/oauth2/*`) + 커스텀 SSO API(`/sso/*`) 동시 지원
- Client Secret은 **SHA-256 HEX** 해시만 DB 저장, 원문은 생성/재발급 시 1회만 표시

---

## 2. 데이터베이스 구조

### SSO 관련 테이블

| 테이블 | 설명 |
|--------|------|
| `ids_iam_sso_key_pair` | RSA-2048 서명 키 쌍 (PEM, 서버 기동 시 자동 생성) |
| `ids_iam_sso_client` | OIDC 클라이언트 설정 (ids_iam_client FK 1:1) |
| `ids_iam_sso_auth_code` | Authorization Code 발행 이력 (5분 유효, ISSUED→USED/EXPIRED) |
| `ids_iam_sso_token` | 발행 토큰 이력 (tokenType: ACCESS/REFRESH/ID) |
| `ids_iam_sso_access_log` | SSO 접근 로그 (action: AUTHORIZE/TOKEN/USERINFO/LOGOUT/CHECK) |

### ids_iam_sso_client 주요 컬럼

| 컬럼 | 설명 |
|------|------|
| `sso_client_oid` | PK (OID 18자) |
| `client_oid` | FK → `ids_iam_client.client_oid` (UNIQUE) |
| `client_id` | OIDC client_id (UNIQUE) |
| `enc_client_secret` | SHA-256 HEX 64자 (원문 미저장) |
| `redirect_uris` | 허용 Redirect URI (줄바꿈 구분) |
| `scopes` | 허용 scope (공백 구분, 기본: `openid profile email`) |
| `grant_types` | 기본: `authorization_code` |
| `access_token_validity_sec` | Access Token 유효시간(초), 기본 3600 |
| `refresh_token_validity_sec` | Refresh Token 유효시간(초), 기본 86400 |
| `id_token_validity_sec` | ID Token 유효시간(초), 기본 3600 |
| `auth_uri` | SSO 세션 없을 때 이동할 인증 페이지 URL |
| `auth_result` | AUTH_CODE 전달받는 콜백 주소 |
| `no_use_sso` | SSO 인증 제외 URI 목록 (줄바꿈 구분) |

---

## 3. 패키지 구조

```
com.idstory.sso
├── entity/
│   ├── SsoKeyPair.java
│   ├── SsoClient.java
│   ├── SsoAuthCode.java
│   ├── SsoToken.java
│   └── SsoAccessLog.java
├── repository/
│   ├── SsoKeyPairRepository.java
│   ├── SsoClientRepository.java
│   ├── SsoAuthCodeRepository.java
│   ├── SsoTokenRepository.java       ← findByJti(), findActiveByClientOidAndUserOid(), findByFilter()
│   └── SsoAccessLogRepository.java
├── service/
│   ├── SsoKeyPairService.java        ← @PostConstruct RSA 키 로드/생성
│   ├── JwtTokenService.java          ← RS256 JWT 생성/검증 (createAccessToken 오버로드)
│   ├── SsoClientService.java         ← 클라이언트 CRUD + verifySecret()
│   ├── SsoAuthCodeService.java       ← 인증코드 발급/검증/USED 처리
│   ├── SsoTokenService.java          ← 토큰 발급/검증/취소/연장
│   └── SsoAccessLogService.java      ← @Async 비동기 접근 로그
├── controller/
│   ├── OidcController.java           ← 표준 OIDC (/oauth2/*)
│   ├── SsoManageController.java      ← 관리 페이지 (/sso/tokens, /sso/auth-codes 등)
│   └── SsoApiController.java         ← 외부 연동 API (/sso/auth, /sso/token 등)
└── dto/
    ├── SsoClientCreateDto.java
    ├── SsoClientUpdateDto.java
    └── OidcTokenResponse.java
```

---

## 4. 클라이언트 관리

### 4.1 APP 유형

클라이언트 등록/수정 시 APP 유형을 선택합니다:

| 유형 | 설명 | 배지 색상 |
|------|------|----------|
| `IAM` | 일반 IAM 클라이언트 (기본값) | 회색 |
| `SSO` | OIDC SSO 연동 클라이언트 | 파랑 |
| `EAM` | EAM 클라이언트 | 초록 |
| `IEAM` | 통합 EAM 클라이언트 | 보라 |

### 4.2 SSO 클라이언트 등록 흐름

1. 클라이언트 등록 모달에서 APP 유형 = **SSO** 선택
2. SSO URI 등록 영역 표시:
   - **SELECT 박스**: AUTH_URI / AUTH_RESULT / REDIRECT_URI 선택
   - **URI 입력 + 추가**: 목록으로 누적 표시, 삭제 가능
3. 저장 → `ids_iam_client` + `ids_iam_sso_client` 동시 생성
4. **Client Secret 1회 표시 모달** 자동 노출 (반드시 복사 보관)

### 4.3 SSO 설정 탭

SSO 유형 클라이언트 선택 → 상세 패널 SSO 설정 탭:

- AUTH_URI / AUTH_RESULT / REDIRECT_URI 목록 관리
- NO_USE_SSO URI 목록 관리
- Token 유효시간 (Access / Refresh / ID)
- [SSO 설정 저장], [Secret 재발급] 버튼

### 4.4 관련 엔드포인트

```
GET  /auth/client/{oid}/sso                   → SSO 설정 조회
POST /auth/client/{oid}/sso/update            → SSO 설정 수정
POST /auth/client/{oid}/sso/regenerate-secret → Secret 재발급
```

---

## 5. OIDC 흐름 (표준 `/oauth2/*`)

표준 OIDC Authorization Code Flow 엔드포인트입니다.

```
[연동 앱]                                [IDStory SSO]
   |                                            |
   |-- GET /oauth2/authorize ------------------>|
   |   ?client_id=xxx                           |  ① 세션 확인 (Spring Security)
   |   &redirect_uri=xxx                        |  ② auth_code 발급 (48자, 5분 유효)
   |   &scope=openid                            |
   |   &state=xxx                               |
   |                                            |
   |<-- 302 redirect_uri?code=xxx&state=--------|  ③ 리다이렉트
   |
   |-- POST /oauth2/token ---------------------->|
   |   Authorization: Basic (clientId:secret)   |  ④ Basic Auth 검증
   |   grant_type=authorization_code            |  ⑤ code 검증 → USED 처리
   |   code=xxx                                 |  ⑥ ACCESS + REFRESH + ID 토큰 발급
   |   redirect_uri=xxx                         |
   |                                            |
   |<-- { access_token, id_token, ... }---------|
   |
   |-- GET /oauth2/userinfo ------------------->|
   |   Authorization: Bearer access_token       |  ⑦ 토큰 검증 → 사용자 정보 반환
   |<-- { sub, name, email, ... }---------------|
```

### 표준 OIDC 공개 엔드포인트

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/.well-known/openid-configuration` | GET | Discovery 문서 |
| `/oauth2/jwks` | GET | JWK Set (RSA 공개키) |
| `/oauth2/authorize` | GET | Authorization 엔드포인트 (인증 필요) |
| `/oauth2/token` | POST | Token 엔드포인트 (form-urlencoded, Basic Auth) |
| `/oauth2/userinfo` | GET | UserInfo 엔드포인트 (Bearer) |

---

## 6. 커스텀 SSO API (`/sso/*`)

표준 OIDC와 동일한 흐름이지만, **JSON 응답 + form-urlencoded 요청** 방식의 커스텀 API입니다.
모든 엔드포인트는 `permitAll()` (인증 불필요) + CSRF 제외 처리됩니다.

---

### 6.1 Authorization Code 발급 — `GET /sso/auth`

> **인증 필요** (미로그인 → Spring Security가 /login 리다이렉트)

**요청 파라미터**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `client_id` | Y | 등록된 OIDC client_id |
| `redirect_uri` | Y | 등록된 redirect_uri 중 하나 |
| `response_type` | Y | `code` 고정 |
| `scope` | N | 기본값: `openid` |
| `state` | N | CSRF 방지용 임의값 |

**성공 응답**: `302 Found` → `redirect_uri?code={auth_code}&state={state}`

**에러 응답**: `302 Found` → `redirect_uri?error={code}&error_description={desc}`

---

### 6.2 토큰 발급 — `POST /sso/token`

JSON(`application/json`) 및 form-urlencoded(`application/x-www-form-urlencoded`) 모두 지원합니다.

#### grant_type=authorization_code

**요청**

```
POST /sso/token
Content-Type: application/x-www-form-urlencoded   (또는 application/json)

grant_type=authorization_code
&client_id=xxx
&client_secret=yyy
&code=zzz
&redirect_uri=https://...
```

**성공 응답** `200 OK`

```json
{
  "access_token":  "eyJ...",
  "token_type":    "Bearer",
  "expires_in":    3600,
  "refresh_token": "eyJ...",
  "id_token":      "eyJ...",
  "scope":         "openid profile email"
}
```

#### grant_type=refresh_token

> Refresh Token Rotation: 기존 refresh_token 즉시 취소 + 새 3종 토큰 발급

**요청**

```
POST /sso/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&client_id=xxx
&client_secret=yyy
&refresh_token=eyJ...
```

**성공 응답** `200 OK` (동일 구조)

**에러 응답** `400/401`

```json
{
  "error": "invalid_grant",
  "error_description": "만료된 Refresh Token입니다."
}
```

---

### 6.3 사용자 정보 조회 — `POST /sso/userinfo`

```
POST /sso/userinfo
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {access_token}

client_id=xxx&client_secret=yyy
```

**성공 응답** `200 OK`

```json
{
  "sub":                "ids_AbcdEfgh123456",
  "name":               "홍길동",
  "preferred_username": "hong",
  "email":              "hong@example.com"
}
```

---

### 6.4 로그아웃 — `POST /sso/logout`

> 해당 client + user의 **모든 활성 토큰** (ACCESS/REFRESH/ID) 일괄 취소

```
POST /sso/logout
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {access_token}

client_id=xxx&client_secret=yyy
```

**성공 응답** `200 OK`

```json
{
  "success": true,
  "message": "로그아웃 되었습니다."
}
```

---

### 6.5 로그인 체크 + 토큰 연장 — `POST /sso/check`

> Access Token 유효성 검증 후 **새 Access Token 재발급** (유효시간 연장)
> 연장 시간: `SYSTEM_POLICY.SSO_ACCESS_TOKEN_EXTEND_SEC` (기본 1800초 = 30분)

**요청**

```
POST /sso/check
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer {access_token}

client_id=xxx&client_secret=yyy
```

**성공 응답** `200 OK`

```json
{
  "valid":        true,
  "access_token": "eyJ... (새로 발급된 JWT)",
  "token_type":   "Bearer",
  "expires_in":   1800,
  "user": {
    "sub":                "ids_AbcdEfgh123456",
    "name":               "홍길동",
    "preferred_username": "hong",
    "email":              "hong@example.com"
  }
}
```

**에러 응답** `401 Unauthorized`

```json
{
  "valid":             false,
  "error":             "invalid_token",
  "error_description": "만료된 토큰입니다."
}
```

**처리 흐름**

```
1. client_id + client_secret 검증
2. Authorization: Bearer {token} 추출
3. SYSTEM_POLICY.SSO_ACCESS_TOKEN_EXTEND_SEC 조회 (기본 1800)
4. JWT 파싱 → JTI 추출
5. DB 레코드 확인 (type=ACCESS, not revoked, not expired, clientOid 일치)
6. 사용자 조회
7. 기존 Access Token revokedAt = now
8. 새 JTI → 새 Access Token 발급 (validitySec = extendSec)
9. DB에 새 레코드 저장
10. ExtendedToken 반환
```

### 연장 시간 정책 변경

관리자 페이지 → **정책 관리** → **시스템** 탭 → `SSO_ACCESS_TOKEN_EXTEND_SEC` 값 수정

---

### 6.6 에러 코드 정리

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `invalid_client` | 401 | client_id/secret 불일치 또는 비활성 |
| `invalid_grant` | 400 | auth_code 또는 refresh_token 유효하지 않음 |
| `invalid_token` | 401 | access_token 무효/만료/취소됨 |
| `invalid_request` | 400 | 필수 파라미터 누락 |
| `unsupported_grant_type` | 400 | 지원하지 않는 grant_type |
| `unsupported_response_type` | 302 | response_type != code |
| `access_denied` | 302 | 비활성 클라이언트 |

---

## 7. JWT 토큰 구조

모든 토큰은 **RS256 (RSA-2048)** 으로 서명됩니다.

### Access Token Claims

```json
{
  "kid": "{key_id}",
  "iss": "http://localhost:9091",
  "sub": "{user_oid}",
  "aud": "{client_id}",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "{uuid}",
  "scope": "openid profile email",
  "client_id": "{client_id}"
}
```

### ID Token Claims

```json
{
  "kid": "{key_id}",
  "iss": "http://localhost:9091",
  "sub": "{user_oid}",
  "aud": "{client_id}",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "{uuid}",
  "name": "{user_name}",
  "preferred_username": "{user_id}",
  "email": "{email}"
}
```

### Refresh Token Claims

```json
{
  "kid": "{key_id}",
  "iss": "http://localhost:9091",
  "sub": "{user_oid}",
  "aud": "{client_id}",
  "exp": 1234567890,
  "iat": 1234567890,
  "jti": "{uuid}",
  "token_use": "refresh"
}
```

---

## 8. Client Secret 관리 정책

- DB에는 **SHA-256 HEX 64자** 만 저장 (`enc_client_secret`)
- 원문 Secret은 **등록/재발급 시 딱 1회만** 화면에 표시
- Secret 복사 후 모달 닫으면 다시 확인 불가 → 분실 시 재발급 필요
- 재발급 시 이전 Secret **즉시 무효화**
- Secret 형식: 영숫자 32자 (`OidGenerator.randomAlphanumeric(32)`)

```java
// SsoClientService.verifySecret()
sha256Hex(rawSecret).equals(client.getEncClientSecret())
```

---

## 9. 관리 페이지 URL

| URL | 설명 |
|-----|------|
| `GET /auth/client` | 클라이언트 관리 (SSO 설정 통합) |
| `GET /sso/tokens` | SSO 토큰 현황 (발행 이력, 단건 취소) |
| `GET /sso/auth-codes` | Authorization Code 발행 이력 |
| `GET /sso/access-log` | SSO 접근 로그 (AUTHORIZE/TOKEN/USERINFO/LOGOUT/CHECK) |

---

## 10. 설정 파일

### application.yml

```yaml
sso:
  issuer: http://localhost:9091   # JWT iss 클레임 / Discovery 문서 issuer
```

### SecurityConfig — CSRF 제외 / permitAll 목록

```java
// CSRF 제외
.csrf(csrf -> csrf.ignoringRequestMatchers(
    "/oauth2/token", "/oauth2/userinfo",
    "/sso/token", "/sso/userinfo", "/sso/logout", "/sso/check"
))

// permitAll
.requestMatchers(
    "/.well-known/openid-configuration", "/oauth2/jwks",
    "/oauth2/token", "/oauth2/userinfo",
    "/sso/token", "/sso/userinfo", "/sso/logout", "/sso/check"
).permitAll()
// /sso/auth, /oauth2/authorize 는 인증 필요 → permitAll에서 제외
```

### RSA 키 자동 관리

- 서버 최초 기동: `SsoKeyPairService.@PostConstruct` → RSA-2048 키 쌍 생성 → `ids_iam_sso_key_pair` 저장
- 이후 기동: DB에서 활성 키 로드 (`active_yn = 'Y'`)
- 키 교체 시: 기존 레코드 `active_yn = 'N'` 처리 후 재기동

### 관련 정책 (ids_iam_policy)

| group | key | 기본값 | 설명 |
|-------|-----|--------|------|
| `SYSTEM_POLICY` | `SSO_ACCESS_TOKEN_EXTEND_SEC` | `1800` | /sso/check 시 Access Token 연장 시간(초) |

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-07 | SSO 기능 최초 구현 (OIDC Authorization Code Flow, RS256 JWT) |
| 2026-03-08 | SSO 클라이언트 관리를 `/auth/client` 페이지에 통합, APP 유형 추가 |
| 2026-03-08 | SSO URI 등록 UI (AUTH_URI/AUTH_RESULT/REDIRECT_URI SELECT + 목록 방식) |
| 2026-03-09 | `/sso/*` 커스텀 API 구현 (SsoApiController) |
| 2026-03-09 | `/sso/token`: JSON + form-urlencoded 이중 지원, Refresh Token Rotation |
| 2026-03-09 | `/sso/userinfo`, `/sso/logout`: form-urlencoded 방식 |
| 2026-03-09 | `/sso/check`: 로그인 체크 + Access Token 연장 (SSO_ACCESS_TOKEN_EXTEND_SEC 정책 연동) |
