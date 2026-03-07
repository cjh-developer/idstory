# IDStory - 변경 이력

> 최신 변경 사항이 위에 기록됩니다.

---

## 2026-03-08 — 통합 정책 관리 + IP/MAC 접근 제어

### 1. 통합 정책 관리 시스템

#### 1-1. DB 설계 (Key-Value + policy_group)
- `ids_iam_policy`: `policy_group + policy_key` 복합PK, `policy_value`, `value_type`, `description`
- `ids_iam_policy_hist`: 정책 변경 이력 (old_value → new_value, changed_by)
- `ids_iam_user.locked_at DATETIME NULL` 컬럼 추가 (자동 해제 기준)
- 기존 `ids_iam_pwd_policy` 유지 (하위 호환)

#### 1-2. SystemPolicyService (신규)
- `getString/getInt/getBoolean(group, key, default)` 헬퍼 메서드
- `saveAll(group, Map<key,value>, changedBy)` — 변경된 값만 저장 + 이력 기록
- `getRecentHistory(limit)` — 최근 정책 변경 이력 조회

#### 1-3. PolicyManageController (신규)
- `GET /policy/manage/{tab}` — 7탭 단일 페이지 (admin|user|password|login|account|audit|system|history)
- `POST /policy/manage/{tab}/save` — 탭별 저장
- VALID_TABS 검증, TAB_TO_GROUP 매핑

#### 1-4. AccountPolicyScheduler (신규)
- `@Scheduled(fixedRate=60_000)` 1분마다 실행
- `ACCT_LOCK_AUTO_RELEASE_MINS` (일반), `ADMIN_LOCK_AUTO_RELEASE_MINS` (관리자) 정책값 기반 자동 잠금 해제
- `SysUser.lockedAt <= now - releaseMins` 조건으로 대상 조회

#### 1-5. SysUserService 정책 연동 추가
| 메서드 | 변경 내용 |
|--------|----------|
| `handleLoginFailure()` | ADMIN/일반 역할 분기, `lockedAt` 설정 |
| `validateUserId()` | USER_ID_MIN/MAX_LEN, REQUIRE_LETTER, START_LETTER, REGEX 검증 |
| `validatePassword()` | PWD_MIN/MAX_LEN, 대소문자/숫자/특수문자 최소 개수, 연속 문자, 금지어 검증 |
| `getInitialPassword()` | PWD_RESET_TYPE=FIXED → 고정값 / RANDOM → 랜덤 alphanumeric |
| `deleteUser()` | USER_HARD_DELETE=true → 물리 삭제, false → 소프트 삭제 |
| `updateUser()` | 잠금 해제 시 `lockedAt = null` |

#### 1-6. 체크박스 버그 수정
- **원인**: `@RequestParam Map<String, String>` → hidden(false)+checkbox(true) 구조에서 첫 번째 값만 저장 → 항상 false
- **수정**: `@RequestParam MultiValueMap<String, String>` + `vals.get(vals.size()-1)`

#### 1-7. manage.html 구조
- 8탭 (7개 정책 + 변경이력) 단일 페이지
- BOOLEAN: `hidden(value=false)` + `checkbox(value=true)` 패턴
- ENUM: `<select>` 요소
- 미구현 항목: 🔒 배지 + disabled 스타일
- 변경 이력 탭: 최근 100건 테이블

#### 변경된 파일
```
신규: com/idstory/policy/entity/SystemPolicy.java, SystemPolicyId.java, SystemPolicyHistory.java
신규: com/idstory/policy/repository/SystemPolicyRepository.java, SystemPolicyHistoryRepository.java
신규: com/idstory/policy/service/SystemPolicyService.java
신규: com/idstory/policy/controller/PolicyManageController.java
신규: com/idstory/policy/scheduler/AccountPolicyScheduler.java
신규: templates/main/policy/manage.html
수정: com/idstory/user/entity/SysUser.java            (lockedAt 필드)
수정: com/idstory/user/service/SysUserService.java    (정책 연동 전반)
수정: com/idstory/user/repository/SysUserRepository.java (findDormantUsers, findLockedBefore)
수정: com/idstory/dashboard/service/DashboardService.java (USER_DORMANT_DAYS 정책 조회)
수정: com/idstory/policy/service/PasswordPolicyService.java (SystemPolicyService 위임)
수정: com/idstory/policy/controller/PasswordPolicyController.java (/policy/manage/password 리다이렉트)
수정: com/idstory/IdstoryApplication.java             (@EnableScheduling)
수정: com/idstory/common/util/OidGenerator.java       (randomAlphanumeric 추가)
수정: scripts/schema.sql, scripts/data.sql
```

---

### 2. IP/MAC 접근 제어

#### 2-1. DB 설계
- `ids_iam_access_control`: rule_oid(PK), control_type(IP|MAC), rule_value, ip_version, description, use_yn
- `ids_iam_access_control_hist`: 차단 이력 (control_type, request_val, request_uri, blocked_at)

#### 2-2. AccessControlService
- `isIpAllowed(clientIp)`: 활성 IP 규칙 없으면 허용(fail-open), CIDR 매칭
- `isMacAllowed(mac)`: 활성 MAC 규칙 없으면 허용, 정규화 후 비교
- CIDR 매칭: `BigInteger` 비트 마스킹으로 IPv4(32bit)/IPv6(128bit) 순수 Java 구현
- MAC 정규화: 구분자 제거 → 대문자 → `AA:BB:CC:DD:EE:FF` 형식
- `recordBlock()`: 차단 시 이력 저장

#### 2-3. AccessControlFilter (OncePerRequestFilter)
- `shouldNotFilter()`: `/css/`, `/js/`, `/images/`, `/font/`, `/favicon`, `/error`, `/login`, `/logout`, `/access-denied`, `/password-reset`, `/auth/access-control/**` 우회
- IP 제어 활성화 → IP 추출(X-Forwarded-For → X-Real-IP → getRemoteAddr()) → 비허용 시 차단
- MAC 제어 활성화 → `X-Device-MAC` 헤더 읽기 → 비허용 시 차단
- AJAX 요청: JSON 403 응답 / 일반 요청: `/access-denied?type=IP&val=xxx` 리다이렉트
- DB 오류 시 fail-open (서비스 접근 허용)

#### 2-4. SecurityConfig 수정
- `AccessControlFilter` 생성자 주입
- `/access-denied` permitAll 추가
- `.addFilterBefore(accessControlFilter, UsernamePasswordAuthenticationFilter.class)`
- `FilterRegistrationBean.setEnabled(false)` — `@Component` 이중 등록 방지

#### 2-5. 순환 의존성 해결
- `AccessControlFilter` 내 `@Lazy AccessControlService` 주입
- SecurityConfig 초기화 시점에 AccessControlService Bean 미생성 문제 해결

#### 2-6. access-control.html UI 개선
- 세션 배너: 현재 접속 IP + X-Device-MAC 헤더 표시
- IP 버전 선택: `<select>` → 세그먼트 컨트롤(pill 버튼) 교체
- 입력 필드: 아이콘 + 힌트 텍스트 추가
- 상태 바: 활성화/비활성화/경고 상태별 색상 배너
- 카드 높이 균등화: `.ac-grid { align-items: stretch }`

#### 변경된 파일
```
신규: com/idstory/accesscontrol/entity/AccessControlRule.java
신규: com/idstory/accesscontrol/entity/AccessControlHist.java
신규: com/idstory/accesscontrol/repository/AccessControlRuleRepository.java
신규: com/idstory/accesscontrol/repository/AccessControlHistRepository.java
신규: com/idstory/accesscontrol/service/AccessControlService.java
신규: com/idstory/accesscontrol/filter/AccessControlFilter.java
신규: com/idstory/accesscontrol/controller/AccessControlController.java
신규: com/idstory/accesscontrol/controller/AccessDeniedPageController.java
신규: templates/main/auth/access-control.html
신규: templates/error/access-denied.html
수정: com/idstory/common/security/SecurityConfig.java
수정: scripts/schema.sql, scripts/data.sql
```

---

## 2026-03-08 — 대시보드 구현 + 역할 사용자 권한 조회 개선

### 1. 권한 사용자 — 간접 권한 조회 + 부서 경고

#### 1-1. 간접 역할 경유 권한 표시 (`PermSubjectService`)
- `getUserPermissions(String userOid)` 추가
  - 직접 배정(`ids_iam_role_user`) + 간접 배정(`ids_iam_role_subject`: DEPT/GRADE/POSITION) 통합 조회
  - 반환: `{userId, userName, rows: [{clientName, permName, permCode, roleName, roleCode, assignedVia}]}`
  - `assignedVia`: `"직접"` / `"간접"` (부서·직위·직급 경유)

#### 1-2. 간접 역할 계산 (`RoleSubjectService`)
- `getUserQualifyingRoleOids(String userOid)` 추가
  - 사용자의 부서(하위 포함) / 직급 / 직위가 `ids_iam_role_subject`에 해당하면 해당 역할 OID 수집
  - EXCEPTION subject는 제외 처리
- `getUserCoveringDeptNames(String roleOid, String userOid)` 추가
  - 특정 역할에서 사용자를 커버하는 부서명 목록 반환 (하위포함 여부 표시)

#### 1-3. 부서 중복 경고 (`RoleUserController`, `role-user.html`)
- `GET /auth/role-user/check-dept?roleOid=&userOid=` 엔드포인트 추가
- 개인 탭에서 사용자 직접 배정 전 부서 중복 여부 사전 확인
- 이미 부서 배정으로 커버되는 경우 `#deptWarningModal` 경고 모달 표시 후 확인 시 진행

#### 1-4. 권한 현황 모달 (`user/list.html`)
- `renderUserPerms()`: 시스템 | 권한명 | 역할명 | **배정 방식** 4열 표시
  - 직접=파란 뱃지, 간접=초록 뱃지

#### 변경된 파일
```
com/idstory/permsubject/service/PermSubjectService.java     (getUserPermissions 추가)
com/idstory/permsubject/controller/PermSubjectController.java (GET /user-perms 추가)
com/idstory/roleuser/service/RoleSubjectService.java        (간접 역할 계산 메서드 추가)
com/idstory/roleuser/controller/RoleUserController.java     (GET /check-dept 추가)
templates/main/auth/role-user.html                          (부서 경고 모달, selectUser 리팩터)
templates/main/user/list.html                               (renderUserPerms 배정방식 열 추가)
```

---

### 2. 대시보드 전면 구현

#### 2-1. Repository 쿼리 추가
| 파일 | 추가 내용 |
|------|-----------|
| `SysUserRepository` | `countByDeletedAtIsNullAndLockYn`, `countByDeletedAtIsNullAndMfaEnabledYn`, `countMonthlyRegistrations()` (네이티브), `findDormantUsers()` (네이티브) |
| `LoginHistoryRepository` | `findTop20ByOrderByPerformedAtDesc`, `countByActionTypeAndPerformedAtAfter`, `findTopFailUsersSince()` (네이티브) |
| `UserAccountHistoryRepository` | `findTop20ByOrderByPerformedAtDesc` |
| `ClientRepository` | `findClientUserCounts()` (네이티브) — 클라이언트별 사용자 수 |
| `SysAdminRepository` | `findRecentGrants(@Param("since"))` — 최근 N일 관리자 권한 변경 |

#### 2-2. DashboardService 신규 생성
- `getStatCounts()` — 전체/활성/비활성/잠금/MFA미등록/관리자 카운트
- `getMonthlyTrend()` — 월별 신규 등록 (최근 12개월)
- `getClientUserCounts()` — 시스템별 사용자 수
- `getRecentLogins()` — 최근 로그인 20건
- `getAccountEvents()` — 최근 계정 이벤트 20건
- `getSecurityAlerts()` — 장기미접속/로그인실패/잠금/관리자변경 4종
- `getAdminList()` — 관리자 목록 (최대 50건)

#### 2-3. MainController 개편
- 페이지: `GET /main/dashboard` (초기 stat 카운트 서버사이드 바인딩)
- API 7종: `/api/stats`, `/api/monthly-trend`, `/api/client-users`, `/api/recent-logins`, `/api/account-events`, `/api/security-alerts`, `/api/admins`

#### 2-4. dashboard.html 전면 재작성
- **Row 1**: 통계 카드 6개 (전체/활성/비활성/잠금/MFA미등록/관리자)
- **Row 2**: Chart.js 월별 추이 라인 차트 + 시스템별 사용자 수 바
- **Row 3**: 최근 로그인 현황 테이블 + 계정 이벤트 로그 테이블
- **Row 4**: 보안 알림 (4종) + 관리자 목록 테이블
- 모든 위젯 AJAX 비동기 로드, 새로고침 버튼

#### 2-5. 버그 수정 — Spring Data JPA `@Query` 위치 오류
- **증상**: `No property 'filter' found for type 'LoginHistory'` / `UserAccountHistory`
- **원인**: `@Query`+`countQuery`가 `findTop20ByOrderByPerformedAtDesc()` 위에 잘못 배치 → `findByFilter`에 annotation 없어 Spring이 메서드명에서 count 파생 시도
- **수정**: `@Query` 블록을 `findByFilter` 바로 위로 이동; `findTop20`은 annotation 없이 유지
- **규칙**: `Page<T>` 반환 메서드에는 반드시 `countQuery` 포함한 `@Query` 명시

#### 2-6. 버그 수정 — 월별 추이 차트 데이터 미표시
- **원인**: JS가 API 반환 데이터만 labels로 사용 → 데이터 없는 달 누락
- **수정**: 12개월 레이블 JS에서 직접 생성 후 countMap으로 빈 달 0 채움

#### 2-7. 높이 균등화
- `.db-row-2` → `align-items: stretch`
- 카드: `display:flex; flex-direction:column`, 카드 바디: `flex:1; min-height:0`
- 목록 컨테이너에 `max-height` + `overflow-y:auto` 적용

#### 변경된 파일
```
com/idstory/user/repository/SysUserRepository.java
com/idstory/history/repository/LoginHistoryRepository.java
com/idstory/history/repository/UserAccountHistoryRepository.java
com/idstory/client/repository/ClientRepository.java
com/idstory/admin/repository/SysAdminRepository.java
com/idstory/dashboard/service/DashboardService.java         (신규 생성)
com/idstory/dashboard/controller/MainController.java        (전면 개편)
templates/main/dashboard.html                               (전면 재작성)
```

---

## 2026-03-04 — UI/UX 전반 개선 (세션 2회차)

### 개요
사용자 관리·관리자 관리·조직/부서·권한·메뉴 관리 화면 전반의 UX를 개선하였습니다.
공통 정책: **목록 화면 헤더 정렬**, **트리 화면 토글 + 스크롤 + 등높이 레이아웃**.

---

### 1. 사용자 관리

#### 1-1. 사용자 목록 (`/user/list`)
- **헤더 클릭 정렬**: 아이디·이름·이메일·휴대번호·부서·역할·사용여부·상태·잠금 9개 컬럼 클릭 시 오름차순/내림차순 토글 (클라이언트 사이드)
- **컬럼 정리**: `생성일` · `담당업무` 컬럼 목록에서 숨김
- **`data-val` 속성**: 각 `<td>`에 정렬 기준값 주입, `sortTable()` JS 함수 추가

#### 1-2. 사용자 등록 (`/user/register`)
- **담당 업무** 텍스트 입력 필드 추가 (선택, maxlength 200)

#### 1-3. 백엔드 — `jobDuty` 필드 추가
| 파일 | 변경 내용 |
|------|-----------|
| `SysUser.java` | `reserveField1` → `jobDuty` 로 필드명 변경 (`@Column(name="reserve_field_1")` 유지) |
| `UserCreateDto.java` | `jobDuty` 필드 추가 |
| `UserUpdateDto.java` | `jobDuty` 필드 추가 |
| `SysUserService.java` | `createUser()` · `updateUser()` 에 `jobDuty` 처리 추가 |

> DB 컬럼 추가 없음 — 기존 `reserve_field_1` 재사용 (`ddl-auto: none` 대응)

---

### 2. 관리자 관리

#### 2-1. 관리자 목록 (`/admin/list`)
- **헤더 클릭 정렬**: 아이디·이름·이메일·휴대번호·부서·사용여부·상태·잠금·등록일 9개 컬럼
- `data-val` 속성 + `sortTable()` JS 함수 추가
- ※ 행 클릭 → 상세 모달(`openEditModal`)은 이전 세션에서 이미 구현되어 있었음

---

### 3. 조직/부서 관련 명칭 변경 및 UI 개선

#### 3-1. 명칭 변경
| 기존 | 변경 후 | 적용 파일 |
|------|---------|-----------|
| 조직도 관리 | **부서 관리** | `org/chart.html`, `scripts/data.sql` |
| 조직사용자 | **부서 사용자** | `org/users.html`, `scripts/data.sql` |

> **DB 적용 필요** (기존 DB에 이미 메뉴가 있는 경우):
> ```sql
> UPDATE ids_iam_menu SET menu_name = '부서 관리'   WHERE url = '/org/chart';
> UPDATE ids_iam_menu SET menu_name = '부서 사용자' WHERE url = '/org/users';
> ```

#### 3-2. 부서 관리 (`/org/chart`) UI 개선
- **트리 토글**: 자식 있는 노드에 `fa-chevron-right/down` 버튼 추가, 클릭 시 접기/펼치기
  - `collapsedSet: Set<string>` 상태 관리, `toggleNode(deptOid)` 함수
  - 검색 중에는 접힌 상태 무시 (검색 결과 전체 노출)
  - 접힌 노드: `fa-folder`, 펼쳐진 노드: `fa-folder-open`
- **등높이 레이아웃**: 컨테이너 `align-items:stretch; height:calc(100vh - 260px)`
- **트리 영역 스크롤**: `#deptTree` → `overflow-y:auto; flex:1; min-height:0`
- **상세 영역 스크롤**: `#deptDetail` → `overflow-y:auto; flex:1; min-height:0`

#### 3-3. 부서 사용자 (`/org/users`) UI 개선
- 위와 동일한 레이아웃 개선 (등높이 + 트리 스크롤 + 우측 스크롤)
- 트리 토글 동일하게 적용 (`collapsedSet`, `toggleNode`)

#### 3-4. 부서장 관리 (`/org/dept-head`) UI 개선
- 레이아웃 등높이 + 스크롤 적용
- 트리 토글 적용 (동일 패턴)
- **상세 패널 정보 확장**: 기존(사용자 OID, 등록일시, 등록자) → 추가 정보 표시
  - 이메일, 휴대번호, 담당 업무, 계정 상태(+잠금 뱃지), 사용여부, 수정일시, 수정자
- **`OrgController.java` API 확장**: `GET /org/api/dept-head` 응답에 SysUser 상세 정보 추가
  - email, phone, jobDuty, useYn, status, lockYn, updatedAt, updatedBy

---

### 4. 권한 관련 화면 개선

#### 4-1. 권한 관리 (`/auth/permission`)
- **첫 클라이언트 자동 선택**: `DOMContentLoaded` 시 `sel.selectedIndex = 1` → `onClientChange()` 호출
- **트리 토글**: `collapsedSet`, `togglePermNode(permOid)` 추가
  - 클라이언트 변경 시 `collapsedSet.clear()` 초기화
- **등높이 레이아웃 + 스크롤**: `height:calc(100vh - 300px)`, 좌우 패널 독립 스크롤

#### 4-2. 권한 설정 (`/auth/setting`)
- **첫 클라이언트 자동 선택**: 동일 패턴 적용

#### 4-3. 권한 사용자 (`/auth/perm-user`)
- **첫 클라이언트 자동 선택**: 동일 패턴 적용

---

### 5. 메뉴 관리 수정 기능 추가 (`/system/menu`)

#### 5-1. 백엔드
| 파일 | 변경 내용 |
|------|-----------|
| `MenuService.java` | `updateMenu(Long menuId, String menuName, String icon, String url)` 메서드 추가 |
| `SystemMenuController.java` | `POST /system/menu/{id}/update` 엔드포인트 추가 |

- 잠금 메뉴(`locked=true`)는 수정 불가 → `IllegalStateException` 발생

#### 5-2. 프론트엔드 (`system/menu.html`)
- **편집(✏) 버튼** 추가: 비잠금 메뉴 "작업" 열에 삭제 버튼 왼쪽에 추가
  - `data-id`, `data-name`, `data-icon`, `data-url` 속성으로 현재 값 전달
- **메뉴 편집 모달** (`#editMenuModal`) 추가:
  - 메뉴명(필수), 아이콘(Font Awesome 클래스 + 실시간 미리보기), URL 입력
  - `POST /system/menu/{id}/update` 제출
- `openEditModal(btn)` · `previewEditIcon(cls)` JS 함수 추가

---

### 변경된 파일 목록

```
src/main/java/com/idstory/
  user/entity/SysUser.java                      (reserveField1 → jobDuty)
  user/dto/UserCreateDto.java                   (jobDuty 추가)
  user/dto/UserUpdateDto.java                   (jobDuty 추가)
  user/service/SysUserService.java              (jobDuty 처리)
  dept/controller/OrgController.java            (dept-head API 확장)
  menu/service/MenuService.java                 (updateMenu 추가)
  menu/controller/SystemMenuController.java     (POST /{id}/update 추가)

src/main/resources/templates/main/
  user/list.html                                (헤더 정렬, 생성일 제거)
  user/register.html                            (담당업무 필드)
  admin/list.html                               (헤더 정렬)
  org/chart.html                                (명칭변경, 트리토글, 스크롤)
  org/users.html                                (명칭변경, 트리토글, 스크롤)
  org/dept-head.html                            (트리토글, 스크롤, 상세 확장)
  auth/permission.html                          (자동선택, 트리토글, 스크롤)
  auth/setting.html                             (자동선택)
  auth/perm-user.html                           (자동선택)
  system/menu.html                              (편집 버튼 + 편집 모달)

scripts/data.sql                                (메뉴명 수정)
```
