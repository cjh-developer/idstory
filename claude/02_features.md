# IDStory - 구현된 기능 목록
> 읽는 순서: 01_structure.md → **02_features.md** → 03_implementation.md

---

## 기능 구현 현황

| # | 기능 | 상태 | 담당 도메인 |
|---|------|------|-------------|
| 1 | 로그인 / 로그아웃 | ✅ 완료 | login, common.security |
| 2 | 비밀번호 초기화 | ✅ 완료 | password |
| 3 | 프로필 드롭다운 (헤더) | ✅ 완료 | include/header.html |
| 4 | 동적 DB 메뉴 (사이드바) | ✅ 완료 | menu |
| 5 | 역할별 메뉴 접근 제어 | ✅ 완료 | menu |
| 6 | 메뉴 관리 페이지 (ADMIN) | ✅ 완료 | menu |
| 7 | 보안 이벤트 로깅 | ✅ 완료 | common.security |
| 8 | 다국어(i18n) 지원 | ✅ 완료 | common.web |
| 9 | 대시보드 | ✅ 완료 (골격) | dashboard |
| 10 | 에러 페이지 (404, 500) | ✅ 완료 | templates/error |
| 11 | 사용자 관리 (목록/등록/수정/삭제) | ✅ 완료 | user |
| 12 | 관리자 계정 관리 | ✅ 완료 | admin |
| 13 | 조직도 관리 (부서 CRUD) | ✅ 완료 | dept |
| 14 | 조직사용자 (부서별 배정 + 겸직) | ✅ 완료 | dept, userorgmap |
| 15 | 겸직 중복 방지 | ✅ 완료 | userorgmap |
| 16 | 직위 관리 (CRUD + 이력) | ✅ 완료 | position, orghistory |
| 17 | 직급 관리 (CRUD + 이력) | ✅ 완료 | grade, orghistory |
| 18 | 직책 관리 (CRUD + 이력) | ✅ 완료 | comprole, orghistory |
| 19 | 부서장 관리 | ✅ 완료 | depthead |
| 20 | 비밀번호 정책 관리 | ✅ 완료 | policy |
| 21 | 로그인 이력 조회 | ✅ 완료 | history |
| 22 | 사용자 계정 이력 조회 | ✅ 완료 | history |
| 23 | 클라이언트 관리 (CRUD + 계층 트리) | ✅ 완료 | client |
| 24 | 역할 관리 (CRUD + 계층 트리) | ✅ 완료 | role |
| 25 | 권한 관리 (클라이언트별 CRUD + 계층 트리) | ✅ 완료 | permission |
| 26 | 권한 설정 (권한↔역할 N:N 배정/해제) | ✅ 완료 | permrole |

---

## 현재 메뉴 구조 (DB 기준)

```
대시보드              (id=1, locked, 전체공개)   /main/dashboard
사용자 관리           (id=2, ADMIN)
  ├── 사용자 목록       /user/list
  ├── 관리자 관리       /admin/list
  ├── 사용자 그룹       # (미구현)
  └── 클라이언트 관리   /auth/client         ← NEW
조직 관리             (id=3, ADMIN)
  ├── 조직도 관리       /org/chart
  ├── 조직사용자        /org/users
  ├── 직위 관리         /org/position
  ├── 직급 관리         /org/grade
  ├── 직책 관리         /org/comp-role
  └── 부서장 관리       /org/dept-head
권한 관리             (id=4, ADMIN)
  ├── 역할 관리         /auth/role           ← NEW
  ├── 권한 관리         /auth/permission     ← NEW
  ├── 접근 제어         # (미구현)
  └── 권한 설정         /auth/setting        ← NEW
인증 관리             (id=5, ADMIN) → 전부 # (미구현)
정책 관리             (id=6, ADMIN)
  └── 비밀번호 정책     /policy/password
시스템 연계           (id=7, ADMIN) → 전부 # (미구현)
감사/이력관리         (id=8, ADMIN)
  ├── 로그인 이력       /history/login
  └── 사용자 계정 이력  /history/user-account
통계/리포트           (id=9, ADMIN) → 전부 # (미구현)
시스템 설정           (id=10, ADMIN)
  └── 메뉴 관리         /system/menu
```

> `url='#'` 서브메뉴는 사이드바에 렌더링되지 않음 (sidebar.html 케이스 ③)

---

## 기능별 상세

### 1. 로그인 / 로그아웃

**URL**
- `GET /login` → 로그인 페이지 (`templates/index.html`)
- `POST /login` → Spring Security 처리 (폼: `username`, `password`)
- `GET /logout` → 로그아웃 (GET 방식 허용)
- `GET /` → `/main/dashboard` 리다이렉트

**인증 주체:** `ids_iam_user` 테이블 기반 (`SysUser` 엔티티)
- 비활성(use_yn=N) / 잠금(lock_yn=Y) / 만료(valid_end_date 초과) 처리
- 로그인 실패 횟수 초과 시 자동 잠금 (policy: MAX_LOGIN_FAIL_COUNT)

---

### 2. 비밀번호 초기화

**URL**
- `GET/POST /password-reset` → 요청 폼
- `GET /password-reset/sent` → 초기화 링크 표시 (데모: 화면에 URL 노출)
- `GET/POST /password-reset/confirm` → 새 비밀번호 입력 및 변경

> ⚠️ 운영 전환 시 JavaMailSender로 이메일 발송 교체 필요

---

### 3. 동적 DB 메뉴 (사이드바)

**처리 흐름**
```
GlobalControllerAdvice.menuTree(Authentication)
  → 역할 추출 (ROLE_ 접두사 제거)
  → MenuService.getMenuTreeByRoles(Set<String>)
  → 필터링 + 트리 빌드
  → ${menuTree} → sidebar.html
```

**사이드바 렌더링 케이스 (3가지)**
1. `children` 있음 → 토글 버튼 + 서브메뉴
2. 자식 없음 + 실제 URL → 직접 링크
3. 자식 없음 + URL 없음/`#` → 렌더링 안 함

---

### 4. 사용자 관리

**URL**
- `GET /user/list` → 사용자 목록 (검색: userId/name/email/role/status)
- `GET/POST /user/register` → 등록 폼 / 처리
- `POST /user/api/{oid}/update` → 수정 (AJAX)
- `POST /user/api/{oid}/delete` → 소프트 삭제 (AJAX)
- `POST /user/api/{oid}/lock` / `unlock` → 잠금/해제
- `POST /user/api/{oid}/reset-password` → 비밀번호 초기화
- `GET/POST /user/api/org-maps/{mapOid}/delete` → 겸직 해제

**특징**
- 등록/수정/삭제/잠금 시 `ids_iam_user_acct_hist` 이력 자동 기록
- 비밀번호: CustomPasswordEncoder (XML 설정 기반 SHA512/HEX)

---

### 5. 관리자 계정 관리

**URL:** `GET /admin/list`

**API**
- `GET /admin/api/search-users` → 사용자 검색 (AJAX)
- `GET /admin/api/{adminOid}` → 단건 조회
- `POST /admin/api/{adminOid}/update` → 비고 수정
- `POST /admin/api/{adminOid}/demote` → 관리자 해제
- `POST /admin/list` → 관리자 등록

---

### 6. 조직도 관리

**URL:** `GET /org/chart`

**API (OrgController)**
```
GET  /org/api/depts                     ← 부서 트리 (includeDeleted 파라미터)
GET  /org/api/depts/{deptOid}           ← 단건 조회
POST /org/api/depts/register            ← 부서 등록
POST /org/api/depts/{deptOid}/update    ← 부서 수정
POST /org/api/depts/{deptOid}/delete    ← 소프트 삭제
POST /org/api/depts/{deptOid}/restore   ← 복원
```

**특징:** 계층형 트리, 소프트 삭제, "삭제된 부서 포함" 토글

---

### 7. 조직사용자 (주소속 + 겸직)

**URL:** `GET /org/users`

**API (OrgController)**
```
GET  /org/api/dept-users?deptOid=&keyword=&page=  ← 부서별 사용자 (10건/페이지)
GET  /org/api/assignable-users?keyword=&page=     ← 배정 가능 사용자 검색
POST /org/api/users/{userOid}/assign              ← 사용자 부서 배정
POST /org/api/users/{userOid}/move-dept           ← 부서 이동
POST /org/api/users/{userOid}/unassign            ← 부서 해제
```

**겸직 (UserOrgMap)**
- `ids_iam_user_org_map` 테이블: `is_primary='Y'` 주소속, `'N'` 겸직
- 겸직 추가: `POST /user/api/{userOid}/org-maps` (UserManagementController)
- 겸직 해제: `POST /user/api/org-maps/{mapOid}/delete`
- **중복 방지:** 동일 부서에 겸직 재등록 시 `"이미 겸직으로 등록된 부서입니다."` 오류

**화면 특징**
- 주소속 사용자 + 겸직 사용자 동시 표시 (겸직 배지 구분)
- 겸직 사용자: "겸직 해제" 버튼, 주소속 사용자: "이동", "해제" 버튼

---

### 8. 부서장 관리

**URL:** `GET /org/dept-head`

**API (OrgController)**
```
GET  /org/api/dept-head?deptOid=         ← 현재 부서장 조회 (없으면 빈 객체 {})
POST /org/api/dept-head                  ← 부서장 등록/교체 (params: deptOid, userOid)
POST /org/api/dept-head/{headOid}/delete ← 부서장 해제
```

**특징**
- 부서당 1명 제한 (`ids_iam_dept_head.dept_oid` UNIQUE)
- 기존 부서장 있을 때 새 사람 등록 → 자동 교체 (삭제 후 신규 저장)
- 2열 레이아웃: 좌측 부서 트리 + 우측 부서장 카드
- 부서장 있음: 이름/아이디/등록일/등록자 카드 + 변경·해제 버튼
- 부서장 없음: 미지정 안내 + 등록 버튼
- 사용자 검색: `/org/api/assignable-users` 재사용

---

### 9. 직위 / 직급 / 직책 관리

**URL:** `/org/position`, `/org/grade`, `/org/comp-role`

**공통 API 패턴 (각 도메인 Controller)**
```
GET  /org/api/{domain}                          ← 목록 (검색 + 페이징)
GET  /org/api/{domain}/{oid}                    ← 단건
POST /org/api/{domain}/register                 ← 등록
POST /org/api/{domain}/{oid}/update             ← 수정
POST /org/api/{domain}/{oid}/delete             ← 소프트 삭제
```

**검색 조건:** 코드 / 이름 / 사용여부(Y/N/전체) / 삭제포함
**이력:** 등록/수정/삭제 시 `ids_iam_org_history`에 JSON 스냅샷 저장
**정렬:** sort_order ASC, code ASC

---

### 10. 비밀번호 정책 관리

**URL:** `GET /policy/password`
**테이블:** `ids_iam_pwd_policy` (policy_key / policy_value)
**현재 정책:** `MAX_LOGIN_FAIL_COUNT=5`

---

### 11. 로그인 이력

**URL:** `GET /history/login`
**테이블:** `ids_iam_login_hist`
**action_type:** `LOGIN_SUCCESS` / `LOGIN_FAIL` / `LOGOUT`
**필터:** actionType / userId / dateFrom / dateTo

---

### 12. 사용자 계정 이력

**URL:** `GET /history/user-account`
**테이블:** `ids_iam_user_acct_hist`
**action_type:** `CREATE` / `UPDATE` / `DELETE` / `LOCK` / `UNLOCK` / `RESET_PWD`
**필터:** actionType / username / dateFrom / dateTo (20건/페이지)

---

## 기능별 상세 — 권한 관리 시스템 (신규)

### 13. 클라이언트 관리

**URL:** `GET /auth/client` (사용자 관리 하위 메뉴)

**API (ClientController)**
```
GET  /auth/client/tree          ← 계층 트리 JSON
GET  /auth/client/{oid}         ← 단건 조회
POST /auth/client               ← 등록 (clientCode, clientName, parentOid, ...)
POST /auth/client/{oid}/update  ← 수정
POST /auth/client/{oid}/delete  ← 소프트 삭제
```

**특징**
- 계층형 트리 (parentOid 자기참조), 소프트 삭제
- 2열 레이아웃: 좌측 트리(320px) + 우측 상세/인라인 수정 패널
- 상위 클라이언트 선택 시 자기 자신 제외 처리

---

### 14. 역할 관리

**URL:** `GET /auth/role`

**API (RoleController)**
```
GET  /auth/role/tree          ← 계층 트리 JSON
GET  /auth/role/{oid}         ← 단건 조회
POST /auth/role               ← 등록 (roleCode, roleName, parentOid, ...)
POST /auth/role/{oid}/update  ← 수정
POST /auth/role/{oid}/delete  ← 소프트 삭제
```

**특징**
- 계층형 트리, 2열 레이아웃 (client.html 동일 패턴)
- 역할 코드 자동 대문자 변환

---

### 15. 권한 관리

**URL:** `GET /auth/permission`

**API (PermissionController)**
```
GET  /auth/permission/tree?clientOid=   ← 클라이언트별 계층 트리
GET  /auth/permission/{oid}             ← 단건 조회
POST /auth/permission                   ← 등록 (clientOid, permCode, permName, ...)
POST /auth/permission/{oid}/update      ← 수정
POST /auth/permission/{oid}/delete      ← 소프트 삭제
```

**특징**
- **클라이언트 선택 필수** — 드롭다운으로 클라이언트 선택 후 해당 권한 트리 로드
- 클라이언트 목록은 서버에서 Model로 전달 (`${clients}`)
- 등록 모달에서 clientOid hidden으로 고정

---

### 16. 권한 설정 (권한-역할 매핑)

**URL:** `GET /auth/setting`

**API (PermRoleController)**
```
GET  /auth/setting/roles?permOid=   ← 배정된/미배정 역할 목록 반환
POST /auth/setting/assign           ← 역할 배정 (permOid, roleOid)
POST /auth/setting/revoke           ← 역할 해제 (permOid, roleOid)
```

**특징**
- **3단 레이아웃**: ① 클라이언트 선택 → ② 권한 트리 → ③ 배정/미배정 역할
- 즉시 반영 (배정/해제 후 패널 자동 갱신)
- `ids_iam_perm_role` 테이블 (perm_oid + role_oid UNIQUE)

---

## 미구현 / 향후 작업 항목

| 항목 | 비고 |
|------|------|
| 접근 제어 (`/auth/**`) | 메뉴·URL 접근 정책 |
| 인증 관리 (`/auth-policy/**`) | MFA, 세션 관리 |
| 시스템 연계 (`/integration/**`) | SSO, API 관리 |
| 통계/리포트 (`/stats/**`) | 사용자·접근 통계 |
| 대시보드 콘텐츠 | 통계 카드, 최근 로그 |
| 비밀번호 초기화 이메일 | JavaMailSender 연동 |
| 사용자 그룹 관리 | `/user/group` |
