# IDStory - 구현 상세 및 작업 가이드
> 읽는 순서: 01_structure.md → 02_features.md → **03_implementation.md**

---

## Spring Security 설정

**파일:** `common/security/SecurityConfig.java`

```java
// 공개 URL (인증 불필요)
"/login", "/password-reset", "/password-reset/**",
"/css/**", "/js/**", "/images/**", "/font/**", "/favicon.ico"

// 로그인
.loginPage("/login")
.loginProcessingUrl("/login")
.defaultSuccessUrl("/", true)    // → LoginController.root() → /main/dashboard
.failureUrl("/login?error=true")

// 로그아웃 (GET 방식 허용)
.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
.logoutSuccessHandler(loggingLogoutSuccessHandler())  // 이력 기록 후 /login?logout=true

// ADMIN 전용 보호
@PreAuthorize("hasRole('ADMIN')")  // 컨트롤러 클래스 레벨에 선언
// @EnableMethodSecurity(prePostEnabled = true) 활성화됨
```

---

## 비밀번호 암호화

**파일:** `common/security/CustomPasswordEncoder.java`, `PasswordXmlProperties.java`
**설정:** `resources/config/password-config.xml`

```xml
<algorithm>SHA512</algorithm>
<encoding>HEX</encoding>
<password-salt-enabled>false</password-salt-enabled>
<password-salt></password-salt>
```

**저장 규칙**
- `salt-enabled=false` → `HASH(password)` → 128자 HEX 소문자
- `salt-enabled=true`  → `HASH(salt + password)` → 128자 HEX 소문자
- `password` 컬럼: 순수 해시값만 저장 (콜론 형식 없음)
- `password_salt` 컬럼: XML salt 값 (enabled=true 시) 또는 NULL

**MySQL 해시 생성 (data.sql)**
```sql
SHA2('1234', 512)                           -- salt disabled
SHA2(CONCAT('saltValue', '1234'), 512)     -- salt enabled
```

---

## OID 생성기

**파일:** `common/util/OidGenerator.java`

```java
public static String generate() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    SecureRandom random = new SecureRandom();
    StringBuilder sb = new StringBuilder("ids_");
    for (int i = 0; i < 14; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
    return sb.toString();  // 총 18자: "ids_" + 14자
}
```

모든 엔티티 PK는 서비스 레이어에서 `OidGenerator.generate()`로 수동 할당.
`@GeneratedValue` 사용 안 함.

---

## 엔티티 패턴

```java
@Entity
@Table(name = "ids_iam_xxx")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Xxx {

    @Id
    @Column(name = "xxx_oid", length = 18)
    private String xxxOid;

    @Column(name = "use_yn", nullable = false, length = 1)
    @Builder.Default
    private String useYn = "Y";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

---

## CRUD 구현 패턴 (AJAX 기반)

### 컨트롤러 표준 패턴
```java
@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class XxxController {

    private final XxxService xxxService;

    // 페이지
    @GetMapping("/xxx")
    public String page(Model model) { return "main/xxx/page"; }

    // 목록 (서버사이드 페이징)
    @GetMapping("/org/api/items")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page) {
        final int PAGE_SIZE = 10;
        Page<Xxx> result = xxxService.findItems(keyword, PageRequest.of(page, PAGE_SIZE));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("content",       result.getContent().stream().map(xxxService::toMap).toList());
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalPages",    result.getTotalPages());
        resp.put("currentPage",   result.getNumber());
        resp.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(resp);
    }

    // 등록
    @PostMapping("/org/api/items/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @ModelAttribute XxxCreateDto dto, BindingResult br,
            Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            xxxService.create(dto, auth.getName());
            result.put("success", true);
            result.put("message", "등록되었습니다.");
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
```

### 서비스 표준 패턴
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService {

    private final XxxRepository repository;
    private final OrgHistoryService orgHistoryService;  // 이력 필요 시

    @Transactional
    public Xxx create(XxxCreateDto dto, String performedBy) {
        Xxx item = Xxx.builder()
                .xxxOid(OidGenerator.generate())
                .createdBy(performedBy)
                .build();
        repository.save(item);
        orgHistoryService.log("XXX", item.getXxxOid(), "CREATE", null, toMap(item), performedBy);
        log.info("[XxxService] 등록 - oid: {}", item.getXxxOid());
        return item;
    }

    @Transactional
    public void softDelete(String oid, String performedBy) {
        Xxx item = getByOid(oid);
        Map<String, Object> before = toMap(item);
        item.setDeletedAt(LocalDateTime.now());
        item.setDeletedBy(performedBy);
        repository.save(item);
        orgHistoryService.log("XXX", oid, "DELETE", before, null, performedBy);
    }

    public Map<String, Object> toMap(Xxx item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("xxxOid", item.getXxxOid());
        m.put("deletedAt", item.getDeletedAt() != null ? item.getDeletedAt().toString() : null);
        return m;
    }

    private String blankToNull(String v) { return (v == null || v.isBlank()) ? null : v; }
}
```

---

## 소프트 삭제 패턴

```java
// 엔티티 필드
@Column(name = "deleted_at") private LocalDateTime deletedAt;
@Column(name = "deleted_by", length = 50) private String deletedBy;

// Repository — 기본 조회: 삭제 제외
@Query("... WHERE p.deletedAt IS NULL ...")

// Repository — 삭제 포함 조회
@Query("... WHERE (:includeDeleted = true OR p.deletedAt IS NULL) ...")
```

---

## 사용자-조직 매핑 (UserOrgMap)

**파일:** `userorgmap/entity/UserOrgMap.java`, `userorgmap/service/UserOrgMapService.java`
**테이블:** `ids_iam_user_org_map`

```java
// 주소속: is_primary = 'Y'
// 겸직:   is_primary = 'N'

// 겸직 중복 방지 (UserOrgMapService.addOrgMap() 시작 부분)
if ("N".equals(isPrimary) && deptOid != null && !deptOid.isBlank()
        && userOrgMapRepository.existsByUserOidAndDeptOidAndIsPrimary(userOid, deptOid, "N")) {
    throw new IllegalArgumentException("이미 겸직으로 등록된 부서입니다.");
}
```

---

## 부서장 관리 (DeptHead)

**파일:** `depthead/entity/DeptHead.java`, `depthead/service/DeptHeadService.java`
**테이블:** `ids_iam_dept_head` (dept_oid UNIQUE → 부서당 1명)

```java
// 등록/교체: 기존 부서장 삭제 후 신규 저장
deptHeadService.assignHead(deptOid, deptName, userOid, userId, userName, performedBy);

// 해제
deptHeadService.removeHead(headOid);
```

**OrgController 엔드포인트**
```
GET  /org/api/dept-head?deptOid=         → 빈 {} 또는 부서장 정보
POST /org/api/dept-head                  → 등록/교체 (deptOid, userOid)
POST /org/api/dept-head/{headOid}/delete → 해제
```

---

## 조직 이력 로깅 (OrgHistory)

**파일:** `orghistory/service/OrgHistoryService.java`
**테이블:** `ids_iam_org_history`

```java
orgHistoryService.log(
    "POSITION",         // targetType: POSITION | GRADE | COMP_ROLE
    item.getOid(),      // targetOid
    "UPDATE",           // actionType: CREATE | UPDATE | DELETE
    before,             // 변경 전 Map (null 허용)
    toMap(item),        // 변경 후 Map (null 허용)
    performedBy
);
```

Jackson ObjectMapper로 Map → JSON 직렬화 → `before_data`, `after_data` TEXT 저장.

---

## 서버사이드 페이징 패턴

### Repository
```java
@Query("""
    SELECT p FROM Position p
    WHERE (:includeDeleted = true OR p.deletedAt IS NULL)
      AND (:code IS NULL OR p.positionCode LIKE %:code%)
    ORDER BY p.sortOrder ASC, p.positionCode ASC
    """)
Page<Position> findByFilter(
    @Param("code") String code,
    @Param("includeDeleted") boolean includeDeleted,
    Pageable pageable);
```

### 프론트엔드 페이징 헬퍼 (buildPagination)
```javascript
// 공통 JS 함수 패턴 (각 페이지 내 선언)
function buildPagination(currentPage, totalPages, onClickFn) {
    if (totalPages <= 1) return '';
    // prev / 번호 (앞뒤 2개 + … 생략) / next 버튼 조합
    return '<div style="display:flex;gap:.3rem;...">...</div>';
}
// 사용: buildPagination(data.currentPage, data.totalPages, 'goPage')
```

---

## AJAX CSRF 처리

```javascript
function appendCsrf(formData) {
    const meta = document.querySelector('meta[name="_csrf"]');
    if (meta) formData.append('_csrf', meta.getAttribute('content'));
}

// POST 요청 시
const fd = new FormData();
fd.append('key', 'value');
appendCsrf(fd);
fetch('/api/endpoint', { method: 'POST', body: fd });
```

---

## 2열 레이아웃 패턴 (조직 관련 페이지 공통)

조직도관리, 조직사용자, 부서장관리 모두 동일 패턴:

```html
<div style="display:flex;gap:1.2rem;align-items:flex-start;">
    <!-- 좌측: 부서 트리 (width:280px, flex-shrink:0) -->
    <div class="card" style="width:280px;flex-shrink:0;min-height:540px;">
        <div id="deptTree">...</div>
    </div>
    <!-- 우측: 콘텐츠 패널 (flex:1) -->
    <div class="card" style="flex:1;min-height:540px;">
        <div id="contentPanel">부서 선택 안내</div>
    </div>
</div>
```

**부서 트리 JS 패턴**
- `loadDeptTree()` → `GET /org/api/depts?includeDeleted=false`
- `renderDeptTree()` → `buildDeptTree(roots, all, depth, matchedOids)` 재귀
- `selectDept(deptOid, deptName)` → 선택 상태 갱신 + 콘텐츠 패널 재로드
- 초기 진입: 루트 부서(본부) 자동 선택

---

## Thymeleaf 레이아웃

```html
<head th:replace="~{include/layout :: head('페이지명')}"></head>
<header th:replace="~{include/header :: header}"></header>
<div class="app-body">
    <aside th:replace="~{include/sidebar :: sidebar}"></aside>
    <main class="app-content">...</main>
</div>
<footer th:replace="~{include/footer :: footer}"></footer>
```

---

## 로깅 컨벤션

```java
// 형식: [클래스명] 동작설명 - 주요파라미터
log.info("[DeptHeadService] 부서장 등록/교체 - deptOid: {}, userOid: {}", deptOid, userOid);
log.info("[UserOrgMapService] 조직 매핑 추가 - userOid: {}, isPrimary: {}", userOid, isPrimary);
log.warn("[SysUserService] 중복 userId - userId: {}", dto.getUserId());
```

---

## 의존성 맵 (도메인 간 참조)

```
common.util (OidGenerator)
  └── ← 모든 서비스 (PK 생성)

common.security (CustomPasswordEncoder + PasswordXmlProperties)
  └── ← user.service, password.service

common.web (GlobalControllerAdvice)
  └── ← menu.service → ${menuTree} → sidebar.html

login.service (CustomUserDetailsService)
  └── ← user.entity, user.repository

dept.controller (OrgController)                ← 조직 관련 API 허브
  ├── ← dept.service (DepartmentService)
  ├── ← user.service (SysUserService)          ← 사용자 배정/조회
  ├── ← userorgmap.service (UserOrgMapService) ← 겸직 관리
  └── ← depthead.service (DeptHeadService)     ← 부서장 관리

orghistory.service (OrgHistoryService)
  └── ← position.service, grade.service, comprole.service

history.service (UserAccountHistoryService)
  └── ← user.service (CRUD 이력)
```

---

## 알려진 주의사항

### 1. blankToNull — FK 컬럼 필수
```java
// FK 컬럼(dept_code 등)에 빈 문자열 삽입 시 FK 무결성 오류
.deptCode(blankToNull(dto.getDeptCode()))  // ✅ 반드시 변환
```

### 2. @Builder.Default + JPA
`@Builder` + `@NoArgsConstructor` 조합 시 JPA 기본 생성자 경로에서 `@Builder.Default`가 적용되지 않을 수 있음.
`@Transient` 필드는 `setChildren(new ArrayList<>())`으로 명시적 초기화 필요.

### 3. Thymeleaf Spring Security 네임스페이스
```html
xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
```
없으면 `sec:authorize` 조용히 무시됨.

### 4. 겸직 중복 방지
`UserOrgMapService.addOrgMap()` 내부에서 `existsByUserOidAndDeptOidAndIsPrimary()` 체크.
동일 사용자가 동일 부서에 겸직 중복 등록 시 `IllegalArgumentException` 발생.

### 5. 부서장 교체 흐름
`DeptHeadService.assignHead()` → 기존 `findByDeptOid()` 있으면 `deleteById()` + `flush()` 후 신규 저장.
UNIQUE 제약(dept_oid)이 있으므로 flush 없이 저장하면 중복 키 오류 발생.

### 6. data.sql 재실행 순서
```sql
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM ids_iam_dept_head;
DELETE FROM ids_iam_user_org_map;
-- ... 기타 테이블
SET FOREIGN_KEY_CHECKS = 1;
```

---

## 권한 관리 도메인 구조 (신규)

### 플로우
```
클라이언트(ids_iam_client)
  └─── 권한(ids_iam_permission, client_oid FK)
         └─── 권한-역할 매핑(ids_iam_perm_role)
                └─── 역할(ids_iam_role)
```

### 계층 트리 서비스 패턴
```java
// 플랫 리스트 → 계층 트리 변환 (ClientService, RoleService, PermissionService 공통)
private List<Client> buildTree(List<Client> all) {
    Map<String, Client> map = new LinkedHashMap<>();
    all.forEach(c -> map.put(c.getClientOid(), c));
    List<Client> roots = new ArrayList<>();
    for (Client c : all) {
        c.setChildren(new ArrayList<>());
        if (c.getParentOid() == null || !map.containsKey(c.getParentOid())) roots.add(c);
        else map.get(c.getParentOid()).getChildren().add(c);
    }
    return roots;
}

// 트리 → Map 직렬화 (children 재귀 포함)
public List<Map<String, Object>> toTreeMapList(List<Client> nodes) { ... }
```

### PermRoleService — 배정/미배정 분리
```java
public Map<String, Object> getRolesForPerm(String permOid) {
    Set<String> assignedOids = permRoleRepository.findByPermOid(permOid)
        .stream().map(PermRole::getRoleOid).collect(toSet());
    List<Role> all = roleRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
    // assigned / unassigned 로 분리하여 반환
}
```

### 권한 관리 API URL 규칙
```
POST /auth/{domain}               ← 등록 (기존 register 대신 루트 POST)
POST /auth/{domain}/{oid}/update  ← 수정
POST /auth/{domain}/{oid}/delete  ← 소프트 삭제
```
> ⚠️ PUT/DELETE 미사용 — CSRF multipart 호환성 이슈로 기존 POST 패턴 통일

### HTML 패턴 — 3단 레이아웃 (setting.html)
```html
<!-- 클라이언트 선택 바 -->
<select onchange="onClientChange()">...</select>

<!-- 3단 flex -->
<div style="display:flex;gap:1.2rem;">
    <!-- 1단: 권한 트리 (width:300px) -->
    <!-- 2단: 배정된 역할 (flex:1) -->
    <!-- 3단: 미배정 역할 (flex:1) -->
</div>
```

### Thymeleaf Model 전달 (클라이언트 선택)
```java
// PermissionController, PermRoleController 공통
@GetMapping
public String page(Model model) {
    model.addAttribute("clients", clientService.findAll());
    return "main/auth/permission";
}
```
```html
<select id="clientSelect">
    <th:block th:each="c : ${clients}">
        <option th:value="${c.clientOid}" th:text="${c.clientName + ' (' + c.clientCode + ')'}"></option>
    </th:block>
</select>
```

---

## 새 도메인 구현 체크리스트

1. `scripts/schema.sql` — `ids_iam_{도메인}` 테이블 DROP/CREATE
2. `scripts/data.sql` — 메뉴 URL `'#'` → 실제 URL 수정, DELETE 절 추가
3. `{domain}/entity/Xxx.java` — `@Entity`, `@PrePersist/@PreUpdate`
4. `{domain}/repository/XxxRepository.java` — `JpaRepository` + 필터 쿼리
5. `{domain}/dto/XxxCreateDto.java`, `XxxUpdateDto.java` — 유효성 애노테이션 포함
6. `{domain}/service/XxxService.java` — CRUD + `orgHistoryService.log()` (이력 필요 시)
7. `{domain}/controller/XxxController.java` — `@PreAuthorize("hasRole('ADMIN')")`
8. `templates/main/{domain}/page.html` — 기존 유사 페이지 패턴 참조
