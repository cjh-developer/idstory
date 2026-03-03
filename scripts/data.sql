-- ============================================================
--  IDStory 통합 로그인 시스템 - DML 스크립트 (초기 데이터)
--  파일 위치 : scripts/data.sql
--  실행 순서 : schema.sql 실행 후 실행하세요
-- ============================================================
--
--  ※ 비밀번호 암호화 기준 (resources/config/password-config.xml)
--     - algorithm             : SHA512
--     - encoding              : HEX
--     - password-salt-enabled : false (기본값)
--     - password-salt         : (비어 있음)
--
--  ※ password-salt-enabled=false 시 MySQL 해시 생성 공식
--     SHA2('입력비밀번호', 512)  →  소문자 HEX 128자
--
--  ※ password-salt-enabled=true 시 MySQL 해시 생성 공식
--     SHA2(CONCAT('설정한salt값', '입력비밀번호'), 512)
--
--  ※ 테스트 계정 목록 (초기 비밀번호 전부 : 1234)
--     ┌───────────────┬──────────┬───────┬────────┬────────┐
--     │ user_id       │ password │ role  │ use_yn │ status │
--     ├───────────────┼──────────┼───────┼────────┼────────┤
--     │ admin         │ 1234     │ ADMIN │   Y    │ ACTIVE │
--     │ user1         │ 1234     │ USER  │   Y    │ ACTIVE │
--     │ user2         │ 1234     │ USER  │   Y    │ ACTIVE │
--     │ disabled_user │ 1234     │ USER  │   N    │ ACTIVE │
--     └───────────────┴──────────┴───────┴────────┴────────┘
-- ============================================================



-- ============================================================
--  기존 데이터 초기화 (재실행 시 중복 방지)
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  ids_iam_dept 초기 데이터
-- ============================================================
INSERT INTO ids_iam_dept (dept_oid, dept_code, dept_name, parent_dept_oid, sort_order, use_yn, created_by) VALUES
                                                                                                               ('ids_Dept0000000001', 'SYSTEM', '본부',   NULL,                  10, 'Y', 'SYSTEM'),
                                                                                                               ('ids_Dept0000000002', 'DEV',    '개발팀', 'ids_Dept0000000001', 20, 'Y', 'SYSTEM'),
                                                                                                               ('ids_Dept0000000003', 'OPS',    '운영팀', 'ids_Dept0000000001', 30, 'Y', 'SYSTEM'),
                                                                                                               ('ids_Dept0000000004', 'PLAN',   '기획팀', 'ids_Dept0000000001', 40, 'Y', 'SYSTEM'),
                                                                                                               ('ids_Dept0000000005', 'ADMIN',  '관리팀', 'ids_Dept0000000001', 50, 'Y', 'SYSTEM');

-- ============================================================
--  ids_iam_user 테스트 계정 (password='1234')
--  password-salt-enabled=false → SHA2('1234', 512)
-- ============================================================

-- ── 관리자 계정 (admin / 1234) ────────────────────────────────────────────────
INSERT INTO ids_iam_user (oid, user_id, name, password, password_salt, email, dept_code, role, use_yn, status, lock_yn, login_fail_count, created_by)
VALUES (
           'ids_AdminOid000001',
           'admin',
           '시스템 관리자',
           SHA2('1234', 512),   -- password-salt-enabled=false → SHA2(password, 512)
           NULL,                -- password-salt-enabled=false → NULL
           'admin@idstory.com',
           'SYSTEM',
           'ADMIN',
           'Y',
           'ACTIVE',
           'N',
           0,
           'SYSTEM'
       );

-- ── 일반 사용자 1 (user1 / 1234) ──────────────────────────────────────────────
INSERT INTO ids_iam_user (oid, user_id, name, password, password_salt, email, dept_code, role, use_yn, status, lock_yn, login_fail_count, created_by)
VALUES (
           'ids_User1Oid000001',
           'user1',
           '일반사용자1',
           SHA2('1234', 512),
           NULL,
           'user1@idstory.com',
           'DEV',
           'USER',
           'Y',
           'ACTIVE',
           'N',
           0,
           'SYSTEM'
       );

-- ── 일반 사용자 2 (user2 / 1234) ──────────────────────────────────────────────
INSERT INTO ids_iam_user (oid, user_id, name, password, password_salt, email, dept_code, role, use_yn, status, lock_yn, login_fail_count, created_by)
VALUES (
           'ids_User2Oid000001',
           'user2',
           '일반사용자2',
           SHA2('1234', 512),
           NULL,
           'user2@idstory.com',
           'OPS',
           'USER',
           'Y',
           'ACTIVE',
           'N',
           0,
           'SYSTEM'
       );

-- ── 비활성 계정 예시 (disabled_user / 1234, use_yn='N') ───────────────────────
INSERT INTO ids_iam_user (oid, user_id, name, password, password_salt, email, dept_code, role, use_yn, status, lock_yn, login_fail_count, created_by)
VALUES (
           'ids_DisableOid0001',
           'disabled_user',
           '비활성사용자',
           SHA2('1234', 512),
           NULL,
           'disabled@idstory.com',
           'PLAN',
           'USER',
           'N',
           'ACTIVE',
           'N',
           0,
           'SYSTEM'
       );

-- ── 결과 확인 ────────────────────────────────────────────────────────────────
SELECT oid, user_id, LEFT(password, 40) AS password_preview,
    name, role, use_yn, status, lock_yn, login_fail_count
FROM ids_iam_user
ORDER BY created_at;

-- ============================================================
--  ids_iam_menu 초기 데이터 (10대 메뉴 + 소분류)
-- ============================================================

-- ── 최상위 메뉴 (locked=1 : 시스템 필수 메뉴, 관리 페이지에서 제어 불가) ──
INSERT INTO ids_iam_menu (menu_id, parent_id, menu_name, icon, url, sort_order, enabled, locked) VALUES
                                                                                                     ( 1, NULL, '대시보드',           'fas fa-gauge-high',        '/main/dashboard', 10,  1, 1),
                                                                                                     ( 2, NULL, '사용자 관리',        'fas fa-users',              NULL,              20,  1, 0),
                                                                                                     ( 3, NULL, '조직 관리',          'fas fa-sitemap',            NULL,              30,  1, 0),
                                                                                                     ( 4, NULL, '권한 관리',          'fas fa-shield-halved',      NULL,              40,  1, 0),
                                                                                                     ( 5, NULL, '인증 관리',          'fas fa-key',                NULL,              50,  1, 0),
                                                                                                     ( 6, NULL, '정책 관리',          'fas fa-file-shield',        NULL,              60,  1, 0),
                                                                                                     ( 7, NULL, '시스템 연계',        'fas fa-plug-circle-bolt',   NULL,              70,  1, 0),
                                                                                                     ( 8, NULL, '감사 / 이력 관리',   'fas fa-clock-rotate-left',  NULL,              80,  1, 0),
                                                                                                     ( 9, NULL, '통계 / 리포트',      'fas fa-chart-bar',          NULL,              90,  1, 0),
                                                                                                     (10, NULL, '시스템 설정',        'fas fa-gear',               NULL,             100,  1, 0);

-- ── 사용자 관리 소분류 ────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (2, '사용자 목록',   NULL, '/user/list',    21, 1),
                                                                                    (2, '관리자 관리',   NULL, '/admin/list',   22, 1),
                                                                                    (2, '사용자 그룹',   NULL, '#',             23, 1),
                                                                                    (2, '클라이언트 관리', NULL, '/auth/client', 24, 1);

-- ── 조직 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (3, '조직도 관리', NULL, '/org/chart',    31, 1),
                                                                                    (3, '조직사용자',  NULL, '/org/users',   32, 1),
                                                                                    (3, '직위 관리',   NULL, '/org/position', 33, 1),
                                                                                    (3, '직급 관리',   NULL, '/org/grade',     34, 1),
                                                                                    (3, '직책 관리',   NULL, '/org/comp-role', 35, 1),
                                                                                    (3, '부서장 관리', NULL, '/org/dept-head', 36, 1);

-- ── 권한 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (4, '역할 관리', NULL, '/auth/role',       41, 1),
                                                                                    (4, '권한 관리', NULL, '/auth/permission', 42, 1),
                                                                                    (4, '접근 제어', NULL, '#',                43, 1),
                                                                                    (4, '권한 설정', NULL, '/auth/setting',    44, 1);

-- ── 인증 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (5, '인증 정책', NULL, '#', 51, 1),
                                                                                    (5, 'MFA 설정',  NULL, '#', 52, 1),
                                                                                    (5, '세션 관리', NULL, '#', 53, 1);

-- ── 정책 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (6, '보안 정책',     NULL, '#', 61, 1),
                                                                                    (6, '비밀번호 정책', NULL, '/policy/password', 62, 1),
                                                                                    (6, '접속 정책',     NULL, '#', 63, 1);

-- ── 시스템 연계 소분류 ───────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (7, '연계 시스템 목록', NULL, '#', 71, 1),
                                                                                    (7, 'API 관리',         NULL, '#', 72, 1),
                                                                                    (7, 'SSO 설정',         NULL, '#', 73, 1);

-- ── 감사 / 이력 관리 소분류 ──────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (8, '로그인 이력',      NULL, '/history/login',         81, 1),
                                                                                    (8, '접근 이력',        NULL, '#',                      82, 1),
                                                                                    (8, '변경 이력',        NULL, '#',                      83, 1),
                                                                                    (8, '사용자 계정 이력', NULL, '/history/user-account',  84, 1);

-- ── 통계 / 리포트 소분류 ─────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (9, '사용자 통계', NULL, '#', 91, 1),
                                                                                    (9, '접근 통계',   NULL, '#', 92, 1),
                                                                                    (9, '보고서 생성', NULL, '#', 93, 1);

-- ── 시스템 설정 소분류 ───────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (10, '기본 설정', NULL, '#',            101, 1),
                                                                                    (10, '알림 설정', NULL, '#',            102, 1),
                                                                                    (10, '백업 관리', NULL, '#',            103, 1),
                                                                                    (10, '메뉴 관리', NULL, '/system/menu', 104, 1);

-- ── ids_iam_menu_role (메뉴 권한 설정) ───────────────────────────────────
--   대시보드(id=1): 권한 없음(empty) = 모든 로그인 사용자에게 표시
--   나머지 관리 메뉴: ADMIN 권한만 표시

-- 최상위 관리 메뉴 → ADMIN 전용
INSERT INTO ids_iam_menu_role (menu_id, role) VALUES
                                                  ( 2, 'ADMIN'), ( 3, 'ADMIN'), ( 4, 'ADMIN'), ( 5, 'ADMIN'),
                                                  ( 6, 'ADMIN'), ( 7, 'ADMIN'), ( 8, 'ADMIN'), ( 9, 'ADMIN'), (10, 'ADMIN');

-- 소분류 메뉴 → 상위 메뉴 역할과 동일하게 ADMIN 전용
INSERT INTO ids_iam_menu_role (menu_id, role)
SELECT m.menu_id, 'ADMIN'
FROM ids_iam_menu m
WHERE m.parent_id IS NOT NULL;

-- ============================================================
--  ids_iam_pwd_policy 초기 데이터
-- ============================================================
INSERT INTO ids_iam_pwd_policy (policy_key, policy_value, description) VALUES
    ('MAX_LOGIN_FAIL_COUNT', '5', '로그인 연속 실패 허용 횟수 (초과 시 계정 자동 잠금)');

-- ============================================================
--  ids_iam_admin 초기 데이터 (admin 계정 매핑)
-- ============================================================
INSERT INTO ids_iam_admin (admin_oid, user_oid, admin_note, granted_by)
VALUES ('ids_SysAdmin000001', 'ids_AdminOid000001', '시스템 초기 관리자', 'SYSTEM');

-- ── 메뉴 결과 확인 ──────────────────────────────────────────────────────
SELECT m.menu_id, p.menu_name AS parent_name, m.menu_name, m.url,
       m.sort_order, m.enabled, m.locked,
       GROUP_CONCAT(mr.role ORDER BY mr.role SEPARATOR ', ') AS roles
FROM ids_iam_menu m
         LEFT JOIN ids_iam_menu p         ON m.parent_id = p.menu_id
         LEFT JOIN ids_iam_menu_role mr   ON m.menu_id   = mr.menu_id
GROUP BY m.menu_id, p.menu_name, m.menu_name, m.url, m.sort_order, m.enabled, m.locked
ORDER BY m.sort_order;
