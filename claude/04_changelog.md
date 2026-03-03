# IDStory - 변경 이력

> 최신 변경 사항이 위에 기록됩니다.

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
