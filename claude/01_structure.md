# IDStory - 프로젝트 구조 가이드
> 이 파일을 먼저 읽어 프로젝트 전체 구조를 파악하세요.
> 읽는 순서: 01_structure.md → 02_features.md → 03_implementation.md

---

## 프로젝트 개요

| 항목 | 값                                                           |
|------|-------------------------------------------------------------|
| 프로젝트명 | IDStory 통합 계정/권한 관리 시스템                                     |
| 기본 패키지 | `com.idstory`                                               |
| 서버 포트 | 9091                                                        |
| DB | MySQL 8.0+ / DB명: `idstory` / 계정: idstory / 비밀번호: idstory01 |
| 빌드 도구 | Gradle (gradlew 포함)                                         |
| Java 버전 | 17                                                          |
| Spring Boot | 3.2.3                                                       |

---

## 전체 디렉터리 구조

```
D:\workspace\idstory\
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── scripts/
│   ├── schema.sql          ← DDL (DB 초기화, 수동 실행)
│   └── data.sql            ← DML (초기 데이터, 수동 실행)
├── docs/
│   └── PROJECT_GUIDE.md    ← 외부 공유용 프로젝트 가이드
├── claude/                 ← Claude 작업 컨텍스트 (이 디렉터리)
│   ├── 01_structure.md
│   ├── 02_features.md
│   └── 03_implementation.md
└── src/main/
    ├── java/com/idstory/
    │   ├── IdstoryApplication.java         ← 메인 클래스 (전체 컴포넌트 스캔)
    │   ├── common/
    │   │   ├── security/                   ← 보안/암호화 공통
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── CustomPasswordEncoder.java
    │   │   │   ├── PasswordXmlProperties.java
    │   │   │   └── AuthEventListener.java
    │   │   ├── web/                        ← MVC/전역 웹 설정
    │   │   │   ├── WebMvcConfig.java
    │   │   │   └── GlobalControllerAdvice.java
    │   │   └── util/                       ← 공통 유틸리티
    │   │       └── OidGenerator.java       ← OID 생성기 (ids_ + 14자)
    │   ├── login/                          ← 로그인 도메인
    │   │   ├── controller/LoginController.java
    │   │   └── service/CustomUserDetailsService.java
    │   ├── password/                       ← 비밀번호 초기화 도메인
    │   │   ├── controller/PasswordResetController.java
    │   │   ├── service/PasswordResetService.java
    │   │   ├── entity/PasswordResetToken.java
    │   │   └── repository/PasswordResetTokenRepository.java
    │   ├── dashboard/                      ← 대시보드 도메인
    │   │   └── controller/MainController.java
    │   ├── menu/                           ← 메뉴 관리 도메인
    │   │   ├── controller/SystemMenuController.java
    │   │   ├── service/MenuService.java
    │   │   ├── entity/Menu.java
    │   │   └── repository/MenuRepository.java
    │   ├── user/                           ← 사용자 관리 도메인
    │   │   ├── controller/UserManagementController.java
    │   │   ├── service/SysUserService.java
    │   │   ├── entity/SysUser.java
    │   │   ├── repository/SysUserRepository.java
    │   │   ├── dto/UserCreateDto.java
    │   │   └── dto/UserUpdateDto.java
    │   ├── admin/                          ← 관리자 계정 도메인
    │   │   ├── controller/AdminManagementController.java
    │   │   ├── service/AdminService.java
    │   │   ├── entity/SysAdmin.java
    │   │   └── repository/SysAdminRepository.java
    │   ├── dept/                           ← 부서/조직 도메인
    │   │   ├── controller/OrgController.java  ← 조직 관련 전체 API 허브
    │   │   ├── service/DepartmentService.java
    │   │   ├── entity/Department.java
    │   │   ├── repository/DepartmentRepository.java
    │   │   ├── dto/DeptCreateDto.java
    │   │   └── dto/DeptUpdateDto.java
    │   ├── depthead/                       ← 부서장 관리 도메인
    │   │   ├── entity/DeptHead.java
    │   │   ├── repository/DeptHeadRepository.java
    │   │   └── service/DeptHeadService.java
    │   ├── userorgmap/                     ← 사용자-조직 매핑 도메인 (주소속/겸직)
    │   │   ├── entity/UserOrgMap.java
    │   │   ├── repository/UserOrgMapRepository.java
    │   │   └── service/UserOrgMapService.java
    │   ├── position/                       ← 직위 관리 도메인
    │   │   ├── controller/PositionController.java
    │   │   ├── service/PositionService.java
    │   │   ├── entity/Position.java
    │   │   ├── repository/PositionRepository.java
    │   │   ├── dto/PositionCreateDto.java
    │   │   └── dto/PositionUpdateDto.java
    │   ├── grade/                          ← 직급 관리 도메인
    │   │   ├── controller/GradeController.java
    │   │   ├── service/GradeService.java
    │   │   ├── entity/Grade.java
    │   │   ├── repository/GradeRepository.java
    │   │   ├── dto/GradeCreateDto.java
    │   │   └── dto/GradeUpdateDto.java
    │   ├── comprole/                       ← 직책 관리 도메인
    │   │   ├── controller/CompRoleController.java
    │   │   ├── service/CompRoleService.java
    │   │   ├── entity/CompRole.java
    │   │   ├── repository/CompRoleRepository.java
    │   │   ├── dto/CompRoleCreateDto.java
    │   │   └── dto/CompRoleUpdateDto.java
    │   ├── orghistory/                     ← 조직 이력 도메인 (직위·직급·직책 통합)
    │   │   ├── service/OrgHistoryService.java
    │   │   ├── entity/OrgHistory.java
    │   │   └── repository/OrgHistoryRepository.java
    │   ├── history/                        ← 감사/이력 도메인
    │   │   ├── controller/LoginHistoryController.java
    │   │   ├── controller/UserHistoryController.java
    │   │   ├── service/LoginHistoryService.java
    │   │   ├── service/UserAccountHistoryService.java
    │   │   ├── entity/LoginHistory.java
    │   │   ├── entity/UserAccountHistory.java
    │   │   ├── repository/LoginHistoryRepository.java
    │   │   └── repository/UserAccountHistoryRepository.java
    │   ├── policy/                         ← 정책 관리 도메인
    │   │   ├── controller/PasswordPolicyController.java
    │   │   ├── service/PasswordPolicyService.java
    │   │   ├── entity/PasswordPolicy.java
    │   │   └── repository/PasswordPolicyRepository.java
    │   ├── client/                         ← 클라이언트(시스템/서비스) 도메인
    │   │   ├── controller/ClientController.java
    │   │   ├── service/ClientService.java
    │   │   ├── entity/Client.java
    │   │   ├── repository/ClientRepository.java
    │   │   ├── dto/ClientCreateDto.java
    │   │   └── dto/ClientUpdateDto.java
    │   ├── role/                           ← 역할 도메인
    │   │   ├── controller/RoleController.java
    │   │   ├── service/RoleService.java
    │   │   ├── entity/Role.java
    │   │   ├── repository/RoleRepository.java
    │   │   ├── dto/RoleCreateDto.java
    │   │   └── dto/RoleUpdateDto.java
    │   ├── permission/                     ← 권한 도메인
    │   │   ├── controller/PermissionController.java
    │   │   ├── service/PermissionService.java
    │   │   ├── entity/Permission.java
    │   │   ├── repository/PermissionRepository.java
    │   │   ├── dto/PermissionCreateDto.java
    │   │   └── dto/PermissionUpdateDto.java
    │   ├── permrole/                       ← 권한-역할 매핑 도메인
    │   │   ├── controller/PermRoleController.java
    │   │   ├── service/PermRoleService.java
    │   │   ├── entity/PermRole.java
    │   │   └── repository/PermRoleRepository.java
    │   └── permsubject/                    ← 권한 대상 도메인 (부서/개인/직급/직위/예외)
    │       ├── controller/PermSubjectController.java
    │       ├── service/PermSubjectService.java
    │       ├── entity/PermSubject.java
    │       └── repository/PermSubjectRepository.java
    └── resources/
        ├── application.yml
        ├── config/
        │   └── password-config.xml         ← 암호화 알고리즘 설정 (SHA512/HEX)
        ├── messages/
        │   ├── messages.properties         ← fallback
        │   ├── messages_ko.properties      ← 한국어 (기본값)
        │   └── messages_en.properties      ← 영어
        ├── static/
        │   ├── css/ (common.css, login.css, main.css)
        │   └── js/  (common.js, login.js)
        └── templates/
            ├── index.html                  ← 로그인 페이지
            ├── include/
            │   ├── layout.html
            │   ├── header.html
            │   ├── sidebar.html
            │   └── footer.html
            ├── login/
            │   ├── password-reset-request.html
            │   ├── password-reset-sent.html
            │   └── password-reset-form.html
            └── main/
                ├── dashboard.html
                ├── home.html
                ├── system/menu.html        ← 메뉴 관리
                ├── user/
                │   ├── list.html           ← 사용자 목록
                │   └── register.html       ← 사용자 등록
                ├── admin/list.html         ← 관리자 목록
                ├── org/
                │   ├── chart.html          ← 조직도 관리
                │   ├── users.html          ← 조직사용자 (주소속+겸직)
                │   ├── dept-head.html      ← 부서장 관리
                │   ├── position.html       ← 직위 관리
                │   ├── grade.html          ← 직급 관리
                │   └── comp-role.html      ← 직책 관리
                ├── auth/
                │   ├── client.html         ← 클라이언트 관리 (2열 트리)
                │   ├── role.html           ← 역할 관리 (2열 트리)
                │   ├── permission.html     ← 권한 관리 (클라이언트 선택 + 2열 트리)
                │   ├── setting.html        ← 권한 설정 (3단: 클라이언트→권한→역할)
                │   └── perm-user.html      ← 권한 사용자 (클라이언트→권한→5탭 대상 배정)
                ├── history/
                │   ├── login.html          ← 로그인 이력
                │   └── user-account.html   ← 사용자 계정 이력
                └── policy/password.html    ← 비밀번호 정책
```

---

## 패키지 구조 규칙 (도메인 기반)

```
com.idstory.[도메인명]/
  ├── controller/   ← @Controller (@PreAuthorize 클래스 레벨)
  ├── service/      ← @Service, 비즈니스 로직
  ├── entity/       ← @Entity, JPA 엔티티 (@PrePersist/@PreUpdate 포함)
  ├── repository/   ← JpaRepository 인터페이스
  └── dto/          ← 폼 입력용 DTO (필요 시)
```

---

## 데이터베이스 스키마

### 테이블 목록 (생성 순서)

| 테이블명 | 설명 |
|---------|------|
| `ids_iam_dept` | 부서 정보 (계층형, 소프트 삭제) |
| `ids_iam_user` | 시스템 사용자 (Spring Security 인증 주체) |
| `ids_iam_dept_head` | 부서장 매핑 (부서당 1명, UNIQUE dept_oid) |
| `ids_iam_admin` | 관리자 추가 정보 (ids_iam_user 1:1) |
| `ids_iam_pwd_reset_token` | 비밀번호 초기화 토큰 |
| `ids_iam_menu` | 동적 메뉴 트리 |
| `ids_iam_menu_role` | 메뉴-역할 접근 제어 |
| `ids_iam_user_acct_hist` | 사용자 계정 이력 |
| `ids_iam_login_hist` | 로그인/로그아웃 이력 |
| `ids_iam_pwd_policy` | 비밀번호 정책 |
| `ids_iam_position` | 직위 관리 |
| `ids_iam_grade` | 직급 관리 |
| `ids_iam_comp_role` | 직책 관리 |
| `ids_iam_user_org_map` | 사용자-조직 매핑 (주소속 Y / 겸직 N) |
| `ids_iam_org_history` | 조직 이력 (직위·직급·직책 변경 로그) |
| `ids_iam_client` | 클라이언트(시스템/서비스) — 계층형, 소프트 삭제 |
| `ids_iam_role` | 역할 — 계층형, 소프트 삭제 |
| `ids_iam_permission` | 권한 — client_oid FK, 계층형, 소프트 삭제 |
| `ids_iam_perm_role` | 권한-역할 매핑 (N:N) |
| `ids_iam_perm_subject` | 권한 대상 매핑 (subject_type: DEPT/USER/GRADE/POSITION/EXCEPTION) |

### DROP 순서 (schema.sql)
```sql
ids_iam_login_hist → ids_iam_user_acct_hist → ids_iam_org_history
→ ids_iam_comp_role → ids_iam_grade → ids_iam_position
→ ids_iam_menu_role → ids_iam_menu → ids_iam_pwd_reset_token
→ ids_iam_admin → ids_iam_dept_head → ids_iam_user_org_map
→ ids_iam_user → ids_iam_dept → ids_iam_pwd_policy
→ ids_iam_perm_subject → ids_iam_perm_role → ids_iam_permission → ids_iam_role → ids_iam_client
→ users
```

### OID 형식
- 형식: `ids_` + 영숫자(A-Za-z0-9) 14자 = 총 **18자** (`CHAR(18)`)
- 생성: `OidGenerator.generate()` (SecureRandom 기반, 서비스 레이어에서 수동 할당)
- `@GeneratedValue` 사용 안 함

### 비밀번호 저장 (ids_iam_user)
- `password` 컬럼 = 순수 해시값 (128자 소문자 HEX)
- `password_salt` 컬럼 = XML salt 값 (salt enabled 시) 또는 NULL
- 설정 파일: `resources/config/password-config.xml`
- 기본값: algorithm=SHA512, encoding=HEX, salt-enabled=false
- MySQL 검증: `SHA2('1234', 512)` (소문자 HEX 128자)

### 테스트 계정 (초기 비밀번호: 1234)

| user_id | role | use_yn | 비고 |
|---------|------|--------|------|
| admin | ADMIN | Y | 관리자 |
| user1 | USER | Y | 일반 사용자 (DEV 부서) |
| user2 | USER | Y | 일반 사용자 (OPS 부서) |
| disabled_user | USER | N | 비활성 계정 (PLAN 부서) |

---

## 핵심 설정 파일

### application.yml 주요 설정
- DB URL: `jdbc:mysql://localhost:3306/idstory_db`
- JPA: `ddl-auto: none`, `show-sql: true`
- 로그 파일: `logs/idstory.log` (10MB 롤링, 30일 보관)

### password-config.xml
```xml
<algorithm>SHA512</algorithm>
<encoding>HEX</encoding>
<password-salt-enabled>false</password-salt-enabled>
```

---

## 빌드 및 실행

```bash
./gradlew compileJava   # 컴파일 검증
./gradlew bootRun       # 서버 실행
# 접속: http://localhost:9091
```

**DB 초기화 순서:** `scripts/schema.sql` → `scripts/data.sql` (수동 실행)
