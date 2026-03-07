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
| 9 | 대시보드 | ✅ 완료 | dashboard |
| 10 | 에러 페이지 (404, 500) | ✅ 완료 | templates/error |
| 11 | 사용자 관리 (목록/등록/수정/삭제) | ✅ 완료 | user |
| 12 | 관리자 계정 관리 | ✅ 완료 | admin |
| 13 | 부서 관리 (부서 CRUD) | ✅ 완료 | dept |
| 14 | 부서 사용자 (부서별 배정 + 겸직) | ✅ 완료 | dept, userorgmap |
| 15 | 겸직 중복 방지 | ✅ 완료 | userorgmap |
| 16 | 직위 관리 (CRUD + 이력) | ✅ 완료 | position, orghistory |
| 17 | 직급 관리 (CRUD + 이력) | ✅ 완료 | grade, orghistory |
| 18 | 직책 관리 (CRUD + 이력) | ✅ 완료 | comprole, orghistory |
| 19 | 부서장 관리 | ✅ 완료 | depthead |
| 20 | 통합 정책 관리 (7카테고리 60+항목) | ✅ 완료 | policy |
| 21 | 로그인 이력 조회 | ✅ 완료 | history |
| 22 | 사용자 계정 이력 조회 | ✅ 완료 | history |
| 23 | 클라이언트 관리 (CRUD + 계층 트리) | ✅ 완료 | client |
| 24 | 역할 관리 (CRUD + 계층 트리) | ✅ 완료 | role |
| 25 | 권한 관리 (클라이언트별 CRUD + 계층 트리) | ✅ 완료 | permission |
| 26 | 권한 설정 (권한↔역할 N:N 배정/해제) | ✅ 완료 | permrole |
| 27 | 권한 사용자 (부서/개인/직급/직위/예외 배정 + 유효사용자 계산) | ✅ 완료 | permsubject |
| 28 | IP/MAC 접근 제어 (필터, 화이트리스트, 차단 이력) | ✅ 완료 | accesscontrol |

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
  ├── 부서 관리         /org/chart
  ├── 부서 사용자       /org/users
  ├── 직위 관리         /org/position
  ├── 직급 관리         /org/grade
  ├── 직책 관리         /org/comp-role
  └── 부서장 관리       /org/dept-head
권한 관리             (id=4, ADMIN)
  ├── 역할 관리         /auth/role
  ├── 권한 관리         /auth/permission
  ├── 접근 제어         /auth/access-control   ← NEW
  ├── 권한 설정         /auth/setting
  └── 권한 사용자       /auth/perm-user
인증 관리             (id=5, ADMIN) → 전부 # (미구현)
정책 관리             (id=6, ADMIN)
  ├── 관리자 정책       /policy/manage/admin   ← NEW
  ├── 사용자 정책       /policy/manage/user    ← NEW
  ├── 비밀번호 정책     /policy/manage/password
  ├── 로그인 정책       /policy/manage/login   ← NEW
  ├── 계정 보안         /policy/manage/account ← NEW
  ├── 감사 로그         /policy/manage/audit   ← NEW
  └── 시스템 보안       /policy/manage/system  ← NEW
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
- 목록 각 행에 🛡 **권한 확인 버튼** → `GET /auth/perm-user/user-perms?userOid=` 호출 → 모달 표시
  (직접 배정 / 부서 경유 / 예외 제외 구분)

---

### 5. 관리자 계정 관리

**URL:** `GET /admin/list`

**API**
- `GET /admin/api/search-users` → 사용자 검색 (AJAX)
- `GET /admin/api/{adminOid}` → 단건 조회
- `POST /admin/api/{adminOid}/update` → 비고 수정
- `POST /admin/api/{adminOid}/demote` → 관리자 해제
- `POST /admin/list` → 관리자 등록

## 직접 수정 내용
```
SysAdminRepository.java 다음과 같이 join 추가

@Query("select a from SysAdmin a join fetch a.user where a.adminOid = :adminOid")
    Optional<SysAdmin> findAdminWithUser(@Param("adminOid") String adminOid);
    
AdminService.java 수정
@Transactional(readOnly = true)
    public SysAdmin getAdminByOid(String adminOid) {
        return adminRepository.findAdminWithUser(adminOid)
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다: " + adminOid));
    }

```




---

### 6. 부서 관리

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

**특징:** 계층형 트리, 소프트 삭제, "삭제된 부서 포함" 토글, 트리 노드 접기/펼치기, 좌우 등높이 스크롤 레이아웃

---

### 7. 부서 사용자 (주소속 + 겸직)

**URL:** `GET /org/users`

**API (OrgController)**
```
GET  /org/api/dept-users?deptOid=&keyword=&page=  ← 부서별 사용자 (10건/페이지, perm-user에서도 재사용)
GET  /org/api/assignable-users?keyword=&page=     ← 배정 가능 사용자 검색 (10건/페이지)
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

### 10. 통합 정책 관리

**URL:** `GET /policy/manage/{tab}` (tab = admin|user|password|login|account|audit|system|history)
**테이블:** `ids_iam_policy` (policy_group + policy_key 복합PK, policy_value, value_type)
**이력 테이블:** `ids_iam_policy_hist`
**기존 호환:** `ids_iam_pwd_policy` 유지, `PasswordPolicyService`가 `SystemPolicyService`에 위임

#### 7개 카테고리 / 60개 항목
| 그룹 | 항목 수 | 주요 정책 |
|------|---------|----------|
| ADMIN_POLICY | 8 | 관리자 MFA, 비밀번호 변경 주기, 최대 실패 횟수, 잠금 자동 해제 |
| USER_POLICY | 8 | ID 길이/형식, 하드삭제 여부, 휴면 일수 |
| PASSWORD_POLICY | 21 | 비밀번호 복잡도, 이력, 초기화 방식, 최대 실패 횟수 |
| LOGIN_POLICY | 7 | 중복 로그인, 이력 보존 기간, IP 제한 |
| ACCOUNT_POLICY | 8 | 잠금 자동 해제, OTP/FIDO/디바이스 인증 |
| AUDIT_POLICY | 5 | 각 이력 자동 삭제 주기 |
| SYSTEM_POLICY | 9 | 세션 타임아웃, API Rate Limit, 파일 업로드, CORS/CSRF |

#### 실제 연동 정책
| 정책 키 | 연동 위치 |
|---------|----------|
| MAX_LOGIN_FAIL_COUNT / ADMIN_MAX_LOGIN_FAIL | `SysUserService.handleLoginFailure()` 역할 분기 |
| PWD_MIN/MAX_LEN, 복잡도, 연속문자, 금지어 | `SysUserService.validatePassword()` |
| USER_ID_MIN/MAX_LEN, REQUIRE_LETTER 등 | `SysUserService.validateUserId()` |
| PWD_RESET_TYPE (FIXED/RANDOM) | `SysUserService.getInitialPassword()` |
| USER_HARD_DELETE | `SysUserService.deleteUser()` soft/hard 분기 |
| USER_DORMANT_DAYS | `DashboardService.getSecurityAlerts()` |
| ACCT_LOCK_AUTO_RELEASE / ADMIN_LOCK_AUTO_RELEASE_MINS | `AccountPolicyScheduler` (@Scheduled 1분) |
| IP_ACCESS_CONTROL_ENABLED / MAC_ACCESS_CONTROL_ENABLED | `AccessControlFilter` |

#### 주요 신규 파일
```
com/idstory/policy/entity/SystemPolicy.java          (@IdClass 복합PK)
com/idstory/policy/entity/SystemPolicyId.java
com/idstory/policy/entity/SystemPolicyHistory.java
com/idstory/policy/repository/SystemPolicyRepository.java
com/idstory/policy/repository/SystemPolicyHistoryRepository.java
com/idstory/policy/service/SystemPolicyService.java
com/idstory/policy/controller/PolicyManageController.java
com/idstory/policy/scheduler/AccountPolicyScheduler.java
templates/main/policy/manage.html
```

#### 체크박스 버그 수정
- **원인**: `@RequestParam Map<String, String>` → 중복 키에서 첫 번째 값만 저장
  - hidden(value=false) + checkbox(value=true) 구조에서 항상 "false"로 저장
- **수정**: `@RequestParam MultiValueMap<String, String>` + `vals.get(vals.size()-1)` 로 마지막 값 사용

---

### 18. IP/MAC 접근 제어

**URL:** `GET /auth/access-control`
**테이블:** `ids_iam_access_control` (규칙), `ids_iam_access_control_hist` (차단 이력)

#### 동작 방식
| 상태 | 등록된 규칙 | 결과 |
|------|------------|------|
| 비활성화 | 관계없음 | 모두 허용 |
| 활성화 | **없음** | 모두 허용 (잠금 방지 fail-open) |
| 활성화 | **있음** | 등록된 항목만 허용, 나머지 차단 → `/access-denied` 리다이렉트 |

#### IP 제어 특징
- IP 추출 순서: `X-Forwarded-For` → `X-Real-IP` → `getRemoteAddr()`
- CIDR 지원: `BigInteger` 비트 마스킹으로 IPv4(32bit)/IPv6(128bit) 순수 Java 구현

#### MAC 제어 특징
- `X-Device-MAC` HTTP 헤더로 MAC 전달 (클라이언트 에이전트/VPN 주입 필요)
- `AA:BB:CC:DD:EE:FF` 대문자 콜론 형식으로 정규화 후 비교

#### 구현 키 포인트
- `@Component` + `FilterRegistrationBean.setEnabled(false)` → 이중 등록 방지
- `@Lazy AccessControlService` 주입 → SecurityConfig 순환 의존성 방지
- 필터 우회: `/css/`, `/js/`, `/login`, `/logout`, `/access-denied`, `/auth/access-control/**`

#### 신규 파일
```
com/idstory/accesscontrol/entity/AccessControlRule.java
com/idstory/accesscontrol/entity/AccessControlHist.java
com/idstory/accesscontrol/repository/AccessControlRuleRepository.java
com/idstory/accesscontrol/repository/AccessControlHistRepository.java
com/idstory/accesscontrol/service/AccessControlService.java
com/idstory/accesscontrol/filter/AccessControlFilter.java
com/idstory/accesscontrol/controller/AccessControlController.java
com/idstory/accesscontrol/controller/AccessDeniedPageController.java
templates/main/auth/access-control.html
templates/error/access-denied.html
```

#### 수정 파일
```
com/idstory/common/security/SecurityConfig.java   (필터 등록 + /access-denied permitAll)
com/idstory/user/entity/SysUser.java              (lockedAt 필드 추가)
com/idstory/user/service/SysUserService.java      (정책 연동, validateUserId/Password, lockedAt)
com/idstory/user/repository/SysUserRepository.java (findDormantUsers, findLockedBefore)
com/idstory/dashboard/service/DashboardService.java (USER_DORMANT_DAYS 정책 조회)
com/idstory/IdstoryApplication.java               (@EnableScheduling)
scripts/schema.sql                                (신규 테이블 4개, locked_at 컬럼)
scripts/data.sql                                  (정책 7개 서브메뉴, 초기 60개 정책 데이터)
```

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

### 17. 권한 사용자

**URL:** `GET /auth/perm-user`

**API (PermSubjectController)**
```
GET  /auth/perm-user/subjects?permOid=&type=   ← 배정된 대상 목록 (type: DEPT|USER|GRADE|POSITION|EXCEPTION)
POST /auth/perm-user/assign                    ← 대상 배정 (permOid, subjectType, subjectOid)
POST /auth/perm-user/revoke                    ← 배정 해제 (permSubjectOid)
GET  /auth/perm-user/effective?permOid=        ← 유효 사용자 계산
GET  /auth/perm-user/user-perms?userOid=       ← 사용자별 권한 현황
```

**화면 레이아웃**
- 클라이언트 선택 바 → 좌측 권한 트리(300px) + 우측 패널
- 우측 패널 상단: 5탭 (부서별 / 개인 / 직급별 / 직위별 / 예외사용자)
- 우측 패널 하단: 유효 사용자 미리보기 (계산 버튼)

**5탭 동작**
| 탭 | subjectType | 선택 모달 | 특이사항 |
|----|-------------|-----------|---------|
| 부서별 | DEPT | 부서 트리 | 카드에 "사용자 토글" 버튼 → `/org/api/dept-users` 호출해 부서원 표시 |
| 개인 | USER | 사용자 선택 (전체 페이징) | 모달 열리면 즉시 전체 목록 로드, 10건/페이지, 검색 지원 |
| 직급별 | GRADE | 직급 목록 | `/org/api/grades?useYn=Y` |
| 직위별 | POSITION | 직위 목록 | `/org/api/positions?useYn=Y` |
| 예외사용자 | EXCEPTION | 사용자 선택 (전체 페이징) | 개인 탭과 동일 모달 재사용 |

**유효사용자 계산 로직 (PermSubjectService.getEffectiveUsers)**
- DEPT: 해당 부서 + 모든 하위 부서 사용자 포함 (재귀)
- USER: 직접 포함
- EXCEPTION: 최종 집합에서 제외
- GRADE/POSITION: `ids_iam_user`에 직급/직위 컬럼 없어 **계산 불가** → `hasGradeOrPositionRule=true` 플래그 반환 + 경고 배너 표시

**사용자별 권한 현황 (getUserPermissions)**
- `GET /auth/perm-user/user-perms?userOid=` 호출 → 사용자의 모든 권한 반환
- **직접 배정** (`ids_iam_role_user`): assignedVia="직접"
- **간접 배정** (`ids_iam_role_subject`: DEPT/GRADE/POSITION 경유): assignedVia="간접"
- 두 경로 통합 후 중복 역할은 직접 배정 우선
- `user/list.html` 각 행의 🛡 권한 확인 버튼으로 진입 → 시스템|권한명|역할명|배정방식 4열 모달 표시

**DB 테이블:** `ids_iam_perm_subject`
```
perm_subject_oid CHAR(18) PK
perm_oid         CHAR(18) FK → ids_iam_permission
subject_type     VARCHAR(20) (DEPT|USER|GRADE|POSITION|EXCEPTION)
subject_oid      CHAR(18)  (부서/사용자/직급/직위 OID)
created_at, created_by
```

---

## 미구현 / 향후 작업 항목

| 항목 | 비고 |
|------|------|
| 인증 관리 (`/auth-policy/**`) | MFA, 세션 관리 |
| 시스템 연계 (`/integration/**`) | SSO, API 관리 |
| 통계/리포트 (`/stats/**`) | 사용자·접근 통계 |
| 비밀번호 초기화 이메일 | JavaMailSender 연동 |
| 사용자 그룹 관리 | `/user/group` |
| 정책 UI 전용 항목 실제 연동 | OTP/FIDO, 감사 로그 자동 삭제 스케줄러, 이상 로그인 탐지 |
