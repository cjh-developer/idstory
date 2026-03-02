# IDStory - 구현된 기능 목록
> 이 파일은 현재 구현된 기능의 전체 목록입니다.
> 읽는 순서: 01_structure.md → **02_features.md** → 03_implementation.md

---

## 기능 구현 현황

| # | 기능 | 상태 | 담당 도메인 |
|---|------|------|-------------|
| 1 | 로그인 / 로그아웃 | ✅ 완료 | login, common.security |
| 2 | 비밀번호 초기화 | ✅ 완료 | password |
| 3 | 프로필 드롭다운 (헤더) | ✅ 완료 | templates/include/header.html |
| 4 | 동적 DB 메뉴 (사이드바) | ✅ 완료 | menu |
| 5 | 역할별 메뉴 접근 제어 | ✅ 완료 | menu |
| 6 | 메뉴 관리 페이지 (ADMIN) | ✅ 완료 | menu |
| 7 | 보안 이벤트 로깅 | ✅ 완료 | common.security |
| 8 | 다국어(i18n) 지원 | ✅ 완료 | common.web |
| 9 | 대시보드 | ✅ 완료 (골격) | dashboard |
| 10 | 에러 페이지 | ✅ 완료 (404, 500) | templates/error |
| 11 | 사용자 관리 (목록/등록/수정/삭제) | ✅ 완료 | user |
| 12 | 관리자 계정 관리 | ✅ 완료 | admin |
| 13 | 조직도 관리 (부서 CRUD) | ✅ 완료 | dept |
| 14 | 조직사용자 (부서별 사용자 배정) | ✅ 완료 | dept, user |
| 15 | 직위 관리 (CRUD + 이력) | ✅ 완료 | position, orghistory |
| 16 | 비밀번호 정책 관리 | ✅ 완료 | policy |
| 17 | 로그인 이력 조회 | ✅ 완료 | history |
| 18 | 사용자 계정 이력 조회 | ✅ 완료 | history |

---

## 현재 메뉴 구조 (DB 기준)

```
대시보드            (id=1, locked, 전체공개)
사용자 관리         (id=2, ADMIN)
  └── 사용자 목록         /user/list
  └── 사용자 등록         /user/register
  └── 관리자 계정 관리    /admin/list
조직 관리           (id=3, ADMIN)
  └── 조직도 관리         /org/chart
  └── 조직사용자          /org/users
  └── 직위 관리           /org/position
  └── 직급 관리           # (미구현)
권한 관리           (id=4, ADMIN) → 대부분 # (미구현)
인증 관리           (id=5, ADMIN) → 대부분 # (미구현)
정책 관리           (id=6, ADMIN)
  └── 비밀번호 정책       /policy/password
감사/이력관리       (id=8, ADMIN)
  └── 로그인 이력         /history/login
  └── 사용자 계정 이력    /history/user-account
시스템 설정         (id=10, ADMIN)
  └── 메뉴 관리           /system/menu
```

> `url='#'` 서브메뉴는 사이드바에 렌더링되지 않음 (케이스 ③)

---

## 기능별 상세

### 1. 로그인 / 로그아웃

**URL**
- `GET /login` → 로그인 페이지 (`templates/index.html`)
- `POST /login` → Spring Security 처리 (폼 파라미터: `username`, `password`)
- `GET /logout` → 로그아웃 (GET 방식 허용)
- `GET /` → `/main/dashboard` 리다이렉트

**인증 주체:** `sys_users` 테이블 기반 (`SysUser` 엔티티)
- 로그인 성공 → `/` → `/main/dashboard` 리다이렉트
- 로그인 실패 → `/login?error=true`
- 비활성(enabled_yn=N) / 잠금(account_status=LOCKED) / 만료(valid_end_date 초과) 처리

---

### 2. 비밀번호 초기화

**URL**
- `GET /password-reset` → 요청 폼
- `POST /password-reset` → 토큰 생성
- `GET /password-reset/sent` → 초기화 링크 표시 (데모: 화면에 URL 노출)
- `GET /password-reset/confirm?token=...` → 새 비밀번호 입력 폼
- `POST /password-reset/confirm` → 비밀번호 변경

**규칙:** 토큰 유효 24시간, UUID 방식, 변경 시 `{salt}:{hash}` 형식으로 저장
> ⚠️ 운영 전환 시 이메일 발송 로직 추가 필요

---

### 3. 동적 DB 메뉴 (사이드바)

**처리 흐름**
```
GlobalControllerAdvice.menuTree(Authentication)
  → 역할 추출 (ROLE_ 접두사 제거)
  → MenuService.getMenuTreeByRoles(Set<String> userRoles)
  → 필터링 + 트리 빌드
  → ${menuTree} → sidebar.html
```

**사이드바 렌더링 케이스 (3가지)**
1. `children` 있음 → 토글 버튼 + 서브메뉴
2. 자식 없음 + 실제 URL → 직접 링크
3. 자식 없음 + URL 없음/`#` → 렌더링 안 함

**역할 제어:** `menu_roles` 행 없음=전체 공개, 있음=해당 역할만

---

### 4. 메뉴 관리 페이지 (ADMIN)

**URL:** `GET/POST /system/menu`

**기능:** 메뉴 추가/활성화토글/삭제/역할편집
- `locked=1` 메뉴 → 토글·삭제·역할수정 불가 (서비스 레이어 강제)

---

### 5. 사용자 관리

**URL**
- `GET /user/list` → 사용자 목록 (검색: username/name/email/role/status)
- `GET /user/register` → 등록 폼 (부서 드롭다운 포함)
- `POST /user/register` → 등록 처리 → redirect:/user/list?success
- `POST /user/api/{oid}/update` → 수정 (AJAX)
- `POST /user/api/{oid}/delete` → 소프트 삭제 (AJAX)
- `POST /user/api/{oid}/lock` / `unlock` → 계정 잠금/해제
- `POST /user/api/{oid}/reset-password` → 비밀번호 초기화

**주요 파일**
- `user/controller/UserManagementController.java`
- `user/service/SysUserService.java`
- `templates/main/user/list.html`, `templates/main/user/register.html`

**특징**
- 부서 없음 선택 시 `dept_code = NULL` (FK 오류 방지: `blankToNull()`)
- 비밀번호 저장: `{salt}:{hash}` 형식 (`CustomPasswordEncoder.encode()`)
- 등록/수정/삭제 시 `user_account_history` 이력 자동 기록

---

### 6. 관리자 계정 관리

**URL:** `GET /admin/list`

**주요 파일**
- `admin/controller/AdminManagementController.java`
- `admin/service/AdminService.java`
- `templates/main/admin/list.html`

---

### 7. 조직도 관리

**URL:** `GET /org/chart`

**API 엔드포인트 (OrgController)**
```
GET  /org/api/depts                         ← 부서 트리 전체 조회
GET  /org/api/depts/{deptOid}               ← 단건 조회
POST /org/api/depts/register                ← 부서 등록
POST /org/api/depts/{deptOid}/update        ← 부서 수정
POST /org/api/depts/{deptOid}/delete        ← 소프트 삭제
```

**특징**
- 계층형 트리 렌더링 (부모-자식 재귀, 삭제된 부서 회색 처리)
- 최초 진입 시 루트 부서(본부) 자동 선택 → 우측 상세 표시
- 소프트 삭제 (`deleted_at`, `deleted_by`)
- dept_oid(OID)와 dept_code(코드) 이중 식별

---

### 8. 조직사용자

**URL:** `GET /org/users`

**API 엔드포인트 (OrgController)**
```
GET  /org/api/dept-users?deptCode=&keyword=&page=  ← 부서별 사용자 (10건/페이지)
GET  /org/api/assignable-users?keyword=&page=      ← 배정 가능 사용자 (10건/페이지)
POST /org/api/dept-users/assign                    ← 사용자 배정
POST /org/api/dept-users/{oid}/remove              ← 배정 해제
```

**특징**
- 좌측: 부서 트리 (최초 루트 부서 자동 선택)
- 우측: 해당 부서 사용자 목록 (서버사이드 페이징, 10건/페이지)
- 배정 모달: 열자마자 전체 목록 표시, 검색 + 페이징 지원
- `buildPagination(currentPage, totalPages, onClickFn)` 공통 JS 함수

---

### 9. 직위 관리

**URL:** `GET /org/position`

**API 엔드포인트 (PositionController)**
```
GET  /org/api/positions                          ← 목록 (검색 + 페이징)
GET  /org/api/positions/{positionOid}            ← 단건
POST /org/api/positions/register                 ← 등록
POST /org/api/positions/{positionOid}/update     ← 수정
POST /org/api/positions/{positionOid}/delete     ← 소프트 삭제
```

**검색 조건:** 직위코드 / 직위명 / 사용여부(Y/N/전체) / 삭제포함 체크박스

**그리드 표시:** No / 직위코드 / 직위명 / 직위상세 / 정렬순서 / 사용여부 / 등록일 / 관리(수정·삭제)
- 삭제된 행: 취소선 + "삭제됨" 배지, 관리 버튼 미표시
- 정렬: sort_order ASC, position_code ASC

**모달:** 등록/수정 Bootstrap 모달 팝업, 삭제는 수정 버튼 옆 별도 버튼

**이력:** 등록/수정/삭제 시 `ids_iam_org_history` 에 JSON 스냅샷 저장
- `OrgHistoryService.log(targetType, targetOid, actionType, before, after, actionBy)`
- before/after: `toMap(Position)` → `ObjectMapper.writeValueAsString(map)` (JSON)

---

### 10. 비밀번호 정책 관리

**URL:** `GET /policy/password`

**주요 파일**
- `policy/controller/PasswordPolicyController.java`
- `policy/service/PasswordPolicyService.java`
- `templates/main/policy/password.html`

---

### 11. 로그인 이력

**URL:** `GET /history/login`

**주요 파일**
- `history/controller/LoginHistoryController.java`
- `history/service/LoginHistoryService.java`
- `templates/main/history/login.html`

---

### 12. 사용자 계정 이력

**URL:** `GET /history/user-account`

**검색 조건:** actionType / dateFrom / dateTo / username

**처리 유형 배지:** CREATE(파란색) / UPDATE(노란색) / DELETE(빨간색) / LOCK(주황색) / UNLOCK(초록색) / RESET_PWD(보라색)

**주요 파일**
- `history/controller/UserHistoryController.java`
- `history/service/UserAccountHistoryService.java`
- `templates/main/history/user-account.html`

---

## 미구현 / 향후 작업 항목

| 항목 | 비고 |
|------|------|
| 직급 관리 (`/org/grade`) | 직위와 동일 패턴, `ids_iam_grade` 테이블 |
| 권한 관리 (`/auth/**`) | 역할·권한·접근제어 |
| 인증 관리 (`/auth-policy/**`) | MFA, 세션 관리 |
| 시스템 연계 (`/integration/**`) | SSO, API 관리 |
| 통계/리포트 (`/stats/**`) | 사용자·접근 통계 |
| 대시보드 콘텐츠 | 통계 카드, 최근 로그 |
| 비밀번호 초기화 이메일 | JavaMailSender 연동 |
| 에러 핸들러 (`common/error/`) | GlobalExceptionHandler |
