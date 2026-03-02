# IDStory - 구현 상세 및 작업 가이드
> 이 파일은 구현 패턴, 주의사항, 다음 작업 방법을 담습니다.
> 읽는 순서: 01_structure.md → 02_features.md → **03_implementation.md**

---

## Spring Security 설정

**파일:** `common/security/SecurityConfig.java`

```java
// 공개 URL (인증 불필요)
"/login", "/password-reset", "/password-reset/**",
"/css/**", "/js/**", "/images/**", "/font/**", "/favicon.ico"

// 로그인 설정
.loginPage("/login")
.loginProcessingUrl("/login")
.defaultSuccessUrl("/", true)   // → LoginController.root() → /main/dashboard
.failureUrl("/login?error=true")

// 로그아웃 (GET 방식 허용)
.logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
.logoutSuccessHandler(loggingLogoutSuccessHandler())  // 로그 후 /login?logout=true

// ADMIN 전용 페이지 보호
@PreAuthorize("hasRole('ADMIN')")  // @EnableMethodSecurity 활성화
```

**새 URL 추가 시:** `/[domain]/**` 경로는 이미 `anyRequest().authenticated()`로 보호됨.
ADMIN 전용은 컨트롤러 클래스에 `@PreAuthorize("hasRole('ADMIN')")` 추가.

---

## 비밀번호 암호화

**파일:** `common/security/CustomPasswordEncoder.java`, `PasswordXmlProperties.java`

### 비밀번호 형식 (sys_users)
```
{8자 salt}:{SHA512 BASE64(salt + rawPassword)}
예) xkL8p3mQ:d404559f...base64...
```

```java
// encode(): salt 자동 생성 후 {salt}:{hash} 반환
@Override
public String encode(CharSequence rawPassword) {
    String salt = generateSalt(8);
    String hash = hashInput(salt + rawPassword.toString());
    return salt + ":" + hash;
}

// matches(): {salt}:{hash} 파싱 지원 + 레거시 fallback
@Override
public boolean matches(CharSequence rawPassword, String encodedPassword) {
    if (encodedPassword.contains(":")) {
        // 새 형식
        String salt = encodedPassword.substring(0, idx);
        String storedHash = encodedPassword.substring(idx + 1);
        return hashInput(salt + rawPassword).equalsIgnoreCase(storedHash);
    }
    // 레거시 (salt 없는 단순 해시)
    return hashInput(rawPassword.toString()).equalsIgnoreCase(encodedPassword);
}
```

### MySQL에서 비밀번호 생성 (data.sql)
```sql
-- salt='TestSalt8', password='1234'
SET @salt = 'TestSalt8';
SET @pw   = '1234';
INSERT INTO sys_users (password, password_salt, ...)
VALUES (CONCAT(@salt, ':', TO_BASE64(UNHEX(SHA2(CONCAT(@salt, @pw), 512)))), @salt, ...);
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
    return sb.toString();  // 총 18자
}
```

모든 엔티티의 PK는 `OidGenerator.generate()` 로 서비스 레이어에서 수동 할당.
`@GeneratedValue` 사용 안 함.

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
    private static final int PAGE_SIZE = 10;

    // 페이지
    @GetMapping("/xxx")
    public String page(Model model) {
        return "main/xxx/page";
    }

    // 목록 (서버사이드 페이징)
    @GetMapping("/xxx/api/items")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String searchParam,
            @RequestParam(defaultValue = "0") int page) {
        Page<Xxx> result = xxxService.findItems(searchParam, PageRequest.of(page, PAGE_SIZE));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content",       result.getContent().stream().map(xxxService::toMap).toList());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages",    result.getTotalPages());
        response.put("currentPage",   result.getNumber());
        response.put("pageSize",      PAGE_SIZE);
        return ResponseEntity.ok(response);
    }

    // 등록
    @PostMapping("/xxx/api/items/register")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> register(
            @Valid @ModelAttribute XxxCreateDto dto, BindingResult br, Authentication auth) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (br.hasErrors()) {
            result.put("success", false);
            result.put("message", br.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        try {
            Xxx item = xxxService.create(dto, auth.getName());
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
                // ... 필드 매핑
                .createdBy(performedBy)
                .build();
        repository.save(item);
        orgHistoryService.log("XXX", item.getXxxOid(), "CREATE", null, toMap(item), performedBy);
        log.info("[XxxService] 등록 - oid: {}", item.getXxxOid());
        return item;
    }

    @Transactional
    public void delete(String oid, String performedBy) {
        Xxx item = getByOid(oid);
        if (item.getDeletedAt() != null) throw new IllegalArgumentException("이미 삭제된 항목입니다.");
        Map<String, Object> before = toMap(item);
        item.setDeletedAt(LocalDateTime.now());
        item.setDeletedBy(performedBy);
        repository.save(item);
        orgHistoryService.log("XXX", oid, "DELETE", before, null, performedBy);
    }

    public Map<String, Object> toMap(Xxx item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("xxxOid",    item.getXxxOid());
        // ... 필드 추가
        m.put("deletedAt", item.getDeletedAt() != null ? item.getDeletedAt().toString() : null);
        return m;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
```

---

## 소프트 삭제 패턴

모든 도메인에서 소프트 삭제 사용:
```java
// 엔티티 필드
@Column(name = "deleted_at") private LocalDateTime deletedAt;
@Column(name = "deleted_by", length = 50) private String deletedBy;

// 서비스에서 삭제
item.setDeletedAt(LocalDateTime.now());
item.setDeletedBy(performedBy);
repository.save(item);
```

```java
// Repository Query - 기본 조회 시 삭제 제외
@Query("SELECT p FROM Position p WHERE p.deletedAt IS NULL ...")
// 삭제 포함 조회 시 includeDeleted boolean 파라미터 사용
@Query("... WHERE (:includeDeleted = true OR p.deletedAt IS NULL) ...")
```

---

## 서버사이드 페이징 패턴

### Repository
```java
@Query("""
    SELECT p FROM Position p
    WHERE (:includeDeleted = true OR p.deletedAt IS NULL)
      AND (:code IS NULL OR p.positionCode LIKE %:code%)
      AND (:name IS NULL OR p.positionName LIKE %:name%)
    ORDER BY p.sortOrder ASC
    """)
Page<Position> findByFilter(
    @Param("code") String code,
    @Param("name") String name,
    @Param("includeDeleted") boolean includeDeleted,
    Pageable pageable);
```

### 프론트엔드 페이징 헬퍼 (공통 JS)
```javascript
function buildPagination(currentPage, totalPages, onClickFn) {
    if (totalPages <= 1) return '';
    let html = '<div style="display:flex;gap:.3rem;...">';
    // prev 버튼
    if (currentPage > 0) html += `<button onclick="${onClickFn}(${currentPage-1})">‹</button>`;
    // 페이지 번호 (앞뒤 2개 + … 생략)
    for (let i = 0; i < totalPages; i++) {
        if (i === currentPage) {
            html += `<button style="background:#2563eb;color:#fff;...">${i+1}</button>`;
        } else if (Math.abs(i - currentPage) <= 2 || i === 0 || i === totalPages-1) {
            html += `<button onclick="${onClickFn}(${i})">${i+1}</button>`;
        } else if (Math.abs(i - currentPage) === 3) {
            html += `<span>…</span>`;
        }
    }
    // next 버튼
    if (currentPage < totalPages - 1) html += `<button onclick="${onClickFn}(${currentPage+1})">›</button>`;
    html += '</div>';
    return html;
}
```

---

## 조직 이력 로깅 패턴

**파일:** `orghistory/service/OrgHistoryService.java`

```java
// 직위/직급 변경 이력 기록
orgHistoryService.log(
    "POSITION",            // targetType: POSITION | GRADE
    p.getPositionOid(),    // targetOid
    "UPDATE",              // actionType: CREATE | UPDATE | DELETE
    before,                // 변경 전 Map (null 허용)
    toMap(p),              // 변경 후 Map (null 허용)
    performedBy            // 처리자 username
);
```

Jackson `ObjectMapper`로 Map → JSON 직렬화 후 `before_data`, `after_data` TEXT 컬럼에 저장.
엔티티 직접 직렬화 X → `toMap()` 경유 (순환 참조·지연 로딩 방지).

---

## 엔티티 패턴

### @PrePersist / @PreUpdate
```java
@Entity
@Table(name = "ids_iam_position")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Position {
    @Id @Column(name = "position_oid", length = 18)
    private String positionOid;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "use_yn", length = 1, nullable = false)
    @Builder.Default
    private String useYn = "Y";

    @Column(name = "created_at", nullable = false, updatable = false)
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

## AJAX CSRF 처리

모든 POST 요청에 CSRF 토큰 첨부:
```javascript
function appendCsrf(formData) {
    const meta = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (meta && header) formData.append(meta.getAttribute('content'), meta.getAttribute('content'));
}

// fetch 요청 시
const fd = new FormData();
fd.append('positionName', positionName);
appendCsrf(fd);  // CSRF 자동 첨부

const resp = await fetch('/org/api/positions/register', {
    method: 'POST',
    body: fd
});
```

```html
<!-- 모든 메인 페이지 <head>에 추가 -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

---

## Thymeleaf 패턴

### 레이아웃 사용
```html
<head th:replace="~{include/layout :: head('페이지 타이틀')}"></head>
<header th:replace="~{include/header :: header}"></header>
<div class="app-body">
    <aside th:replace="~{include/sidebar :: sidebar}"></aside>
    <main class="app-content">...</main>
</div>
<footer th:replace="~{include/footer :: footer}"></footer>
```

### th:onclick 주의사항
```html
<!-- ❌ 오류 발생 -->
th:onclick="'func(' + ${item.name} + ')'"

<!-- ✅ 올바른 방법 -->
th:attr="data-oid=${item.positionOid}, data-name=${item.positionName}"
onclick="openEditModal(this)"

<!-- JS에서 -->
function openEditModal(btn) {
    const oid  = btn.dataset.oid;
    const name = btn.dataset.name;
}
```

---

## JPA 사용 패턴

### 트랜잭션
```java
@Service
@Transactional(readOnly = true)  // 기본: 읽기 전용
public class PositionService {
    @Transactional  // 쓰기 작업에만 override
    public Position createPosition(PositionCreateDto dto, String performedBy) { ... }
}
```

### blankToNull 유틸
```java
private String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
}
```
FK 컬럼(dept_code 등)에 빈 문자열 삽입 시 FK 무결성 오류 방지.
`deptCode` 처럼 선택적 FK 필드는 반드시 `blankToNull()` 통해 null 변환 후 저장.

---

## 모달 팝업 패턴

### Bootstrap 5 모달 레이아웃 (스크롤 가능)
```html
<div class="modal fade" id="registerModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-scrollable"
         style="display:flex;flex-direction:column;">
        <div class="modal-content" style="flex:1;overflow:hidden;">
            <div class="modal-header">...</div>
            <div class="modal-body" style="overflow-y:auto;">
                <!-- 폼 필드 -->
            </div>
            <div class="modal-footer">
                <button class="btn btn-secondary" data-bs-dismiss="modal">취소</button>
                <button class="btn btn-primary" onclick="doRegister()">등록</button>
            </div>
        </div>
    </div>
</div>
```

---

## 새 도메인 구현 체크리스트

예시: **직급 관리** (`/org/grade`) 구현 시

1. DB: `ids_iam_grade` 테이블 생성 (position과 동일 구조)
2. `grade/entity/Grade.java` — @Entity @Table(ids_iam_grade)
3. `grade/repository/GradeRepository.java` — findByFilter
4. `grade/dto/GradeCreateDto.java`, `GradeUpdateDto.java`
5. `grade/service/GradeService.java` — CRUD + orgHistoryService.log("GRADE", ...)
6. `grade/controller/GradeController.java` — @PreAuthorize("hasRole('ADMIN')")
7. `templates/main/org/grade.html` — position.html 패턴 그대로 복제
8. `data.sql`: 직급 관리 메뉴 url `'#'` → `'/org/grade'`

---

## 로깅 컨벤션

```java
// 형식: [클래스명] 동작설명 - 주요파라미터
log.info("[PositionService] 직위 등록 - oid: {}, code: {}", p.getPositionOid(), p.getPositionCode());
log.info("[PositionService] 직위 수정 - oid: {}", p.getPositionOid());
log.warn("[SysUserService] 중복 username - username: {}", dto.getUsername());
```

---

## CSS 시스템

### CSS 변수 (main.css 최상단)
```css
:root {
  --sidebar-width: 245px;
  --header-height: 56px;
  --blue-500: #3b82f6;
  --blue-600: #2563eb;
  --gray-50 ~ --gray-900;
  --amber-600: #d97706;
}
```

### 레이아웃 구조
```css
.app-wrapper          /* 전체 래퍼 */
  .app-sidebar        /* 좌측 사이드바 (width: 245px) */
  .app-body           /* 메인 영역 */
    .app-content      /* 콘텐츠 (margin-left: 245px) */
  .app-footer
```

---

## 알려진 주의사항

### 1. DB 재초기화 순서
```sql
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ids_iam_org_history;
DROP TABLE IF EXISTS ids_iam_position;
DROP TABLE IF EXISTS user_account_history;
DROP TABLE IF EXISTS menu_roles;
DROP TABLE IF EXISTS menus;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS sys_users;
DROP TABLE IF EXISTS departments;
SET FOREIGN_KEY_CHECKS = 1;
-- CREATE 순서: departments → sys_users → password_reset_tokens → menus → menu_roles
--             → ids_iam_position → ids_iam_org_history → user_account_history → login_history
```

### 2. 부서 없음 FK 오류 방지
```java
// ❌ 빈 문자열 "" 그대로 FK 컬럼에 삽입 시 오류
.deptCode(dto.getDeptCode())

// ✅ blankToNull()로 변환
.deptCode(blankToNull(dto.getDeptCode()))
```

### 3. Thymeleaf 내 Spring Security
- `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` 반드시 선언
- `sec:authorize` 없으면 조용히 무시됨 (주의)

### 4. @Builder.Default + JPA
- `@Builder` + `@NoArgsConstructor` 조합 시 `@Builder.Default` 초기화가 JPA 인스턴스에서 적용되지 않을 수 있음
- `@Transient` 필드는 `setChildren(new ArrayList<>())` 로 명시적 초기화 필요

### 5. 비밀번호 초기화 운영 전환
- 현재: 화면에 토큰 URL 직접 표시 (데모용)
- 운영 전: `PasswordResetService.createResetToken()` 에서 `JavaMailSender`로 교체

---

## 의존성 맵 (도메인 간 참조)

```
common.util (OidGenerator)
  └── ← user.service, dept.service, position.service, orghistory.service, history.service

common.security (CustomPasswordEncoder)
  └── ← user.service, password.service

common.web (GlobalControllerAdvice)
  └── ← menu.service, menu.entity

login.service (CustomUserDetailsService)
  └── ← user.entity (SysUser), user.repository (SysUserRepository)

password.service (PasswordResetService)
  └── ← user.entity (SysUser), user.repository (SysUserRepository)
  └── ← common.security (CustomPasswordEncoder)

orghistory.service (OrgHistoryService)
  └── ← position.service (이력 로깅)
  └── ← (향후 grade.service)

history.service (UserAccountHistoryService)
  └── ← user.service (사용자 CRUD 이력)

dept.controller (OrgController)
  └── ← dept.service (DepartmentService)
  └── ← user.service (SysUserService)   ← 조직사용자 배정/조회

menu ← (독립)
dashboard ← (독립)
admin ← (독립)
policy ← (독립)
```
