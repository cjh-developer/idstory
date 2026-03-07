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
                                                                                    (3, '부서 관리',   NULL, '/org/chart',    31, 1),
                                                                                    (3, '부서 사용자', NULL, '/org/users',   32, 1),
                                                                                    (3, '직위 관리',   NULL, '/org/position', 33, 1),
                                                                                    (3, '직급 관리',   NULL, '/org/grade',     34, 1),
                                                                                    (3, '직책 관리',   NULL, '/org/comp-role', 35, 1),
                                                                                    (3, '부서장 관리', NULL, '/org/dept-head', 36, 1);

-- ── 권한 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (4, '역할 관리', NULL, '/auth/role',       41, 1),
                                                                                    (4, '권한 관리', NULL, '/auth/permission', 42, 1),
                                                                                    (4, '접근 제어', NULL, '/auth/access-control', 43, 1),
                                                                                    (4, '권한 설정',  NULL, '/auth/setting',    44, 1),
                                                                                    (4, '역할 사용자', NULL, '/auth/role-user',  45, 1);

-- ── 인증 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
                                                                                    (5, '인증 정책', NULL, '#', 51, 1),
                                                                                    (5, 'MFA 설정',  NULL, '#', 52, 1),
                                                                                    (5, '세션 관리', NULL, '#', 53, 1);

-- ── 정책 관리 소분류 ─────────────────────────────────────────────────────
INSERT INTO ids_iam_menu (parent_id, menu_name, icon, url, sort_order, enabled) VALUES
    (6, '관리자 정책',   NULL, '/policy/manage/admin',    61, 1),
    (6, '사용자 정책',   NULL, '/policy/manage/user',     62, 1),
    (6, '비밀번호 정책', NULL, '/policy/manage/password', 63, 1),
    (6, '로그인 정책',   NULL, '/policy/manage/login',    64, 1),
    (6, '계정 보안',     NULL, '/policy/manage/account',  65, 1),
    (6, '감사 로그',     NULL, '/policy/manage/audit',    66, 1),
    (6, '시스템 보안',   NULL, '/policy/manage/system',   67, 1);

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
--  ids_iam_policy 초기 데이터 (60개 항목)
-- ============================================================

-- ADMIN_POLICY (8개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('ADMIN_POLICY', 'ADMIN_MFA_ENABLED',           'false', 'BOOLEAN',      '관리자 2차 인증 활성화'),
    ('ADMIN_POLICY', 'ADMIN_PWD_CHANGE_PERIOD',     '90',    'INTEGER',      '관리자 비밀번호 변경 주기(일, 0=비활성)'),
    ('ADMIN_POLICY', 'ADMIN_MAX_LOGIN_FAIL',         '5',     'INTEGER',      '관리자 로그인 최대 실패 횟수'),
    ('ADMIN_POLICY', 'ADMIN_LOCK_AUTO_RELEASE_MINS', '0',     'INTEGER',      '관리자 계정 잠금 자동 해제(분, 0=비활성)'),
    ('ADMIN_POLICY', 'ADMIN_MAX_IP_ALLOW',           '0',     'INTEGER',      '관리자 허용 IP 수(0=무제한)'),
    ('ADMIN_POLICY', 'ADMIN_STATUS_CHECK',           'true',  'BOOLEAN',      '관리자 로그인 시 상태 체크'),
    ('ADMIN_POLICY', 'ADMIN_SESSION_TIMEOUT',        '60',    'INTEGER',      '관리자 세션 유지 시간(분)'),
    ('ADMIN_POLICY', 'ADMIN_ALLOW_COUNTRIES',        '',      'MULTI_STRING', '관리자 접속 허용 국가(콤마 구분, 비어있으면 무제한)');

-- USER_POLICY (8개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('USER_POLICY', 'USER_ID_MIN_LEN',       '4',     'INTEGER', '사용자 ID 최소 길이'),
    ('USER_POLICY', 'USER_ID_MAX_LEN',       '20',    'INTEGER', '사용자 ID 최대 길이'),
    ('USER_POLICY', 'USER_ID_REQUIRE_LETTER','true',  'BOOLEAN', '사용자 ID에 영문자 포함 필수'),
    ('USER_POLICY', 'USER_ID_START_LETTER',  'false', 'BOOLEAN', '사용자 ID를 영문자로 시작'),
    ('USER_POLICY', 'USER_ID_REGEX',         '',      'STRING',  '사용자 ID 정규식 패턴(비어있으면 미적용)'),
    ('USER_POLICY', 'USER_HARD_DELETE',      'false', 'BOOLEAN', '사용자 하드 삭제 여부(false=소프트 삭제)'),
    ('USER_POLICY', 'USER_DORMANT_DAYS',     '90',    'INTEGER', '휴면 계정 기준 미접속 일수'),
    ('USER_POLICY', 'USER_INACTIVE_DAYS',    '180',   'INTEGER', '비활성 계정 전환 기준 일수');

-- PASSWORD_POLICY (21개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('PASSWORD_POLICY', 'PWD_MIN_LEN',           '8',              'INTEGER',      '비밀번호 최소 길이'),
    ('PASSWORD_POLICY', 'PWD_MAX_LEN',           '100',            'INTEGER',      '비밀번호 최대 길이'),
    ('PASSWORD_POLICY', 'PWD_MIN_UPPER',         '0',              'INTEGER',      '대문자 최소 포함 개수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_MIN_LOWER',         '0',              'INTEGER',      '소문자 최소 포함 개수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_MIN_DIGIT',         '0',              'INTEGER',      '숫자 최소 포함 개수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_MIN_SPECIAL',       '0',              'INTEGER',      '특수문자 최소 포함 개수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_ALLOWED_SPECIAL',   '!@#$%^&*()_+-=', 'STRING',       '허용 특수문자 목록'),
    ('PASSWORD_POLICY', 'PWD_MAX_REPEAT',        '0',              'INTEGER',      '동일 문자 최대 연속 반복(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_HISTORY_COUNT',     '0',              'INTEGER',      '비밀번호 이력 재사용 금지 개수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_RESET_VALUE',       '1234',           'STRING',       '초기화 비밀번호 고정값'),
    ('PASSWORD_POLICY', 'PWD_RESET_TYPE',        'FIXED',          'ENUM',         '초기화 비밀번호 방식(FIXED|RANDOM)'),
    ('PASSWORD_POLICY', 'PWD_CHANGE_PERIOD',     '0',              'INTEGER',      '비밀번호 변경 주기(일, 0=비활성)'),
    ('PASSWORD_POLICY', 'PWD_EXTEND_PERIOD',     '7',              'INTEGER',      '비밀번호 만료 후 연장 가능 일수'),
    ('PASSWORD_POLICY', 'MAX_LOGIN_FAIL_COUNT',  '5',              'INTEGER',      '로그인 최대 실패 횟수(초과 시 잠금)'),
    ('PASSWORD_POLICY', 'PWD_SALT_ENABLED',      'false',          'BOOLEAN',      '비밀번호 Salt 사용 여부'),
    ('PASSWORD_POLICY', 'PWD_SALT_LEN',          '8',              'INTEGER',      'Salt 길이(RANDOM 시)'),
    ('PASSWORD_POLICY', 'PWD_CHECK_PERIOD',      '0',              'INTEGER',      '비밀번호 강도 검사 주기(일, 0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_NO_CONSECUTIVE',    'false',          'BOOLEAN',      '연속 문자 사용 금지'),
    ('PASSWORD_POLICY', 'PWD_FORBIDDEN_WORDS',   '',               'MULTI_STRING', '금지 단어 목록(콤마 구분)'),
    ('PASSWORD_POLICY', 'PWD_MIN_SCORE',         '0',              'INTEGER',      '최소 비밀번호 강도 점수(0=미검사)'),
    ('PASSWORD_POLICY', 'PWD_MAX_FAIL_LOCK',     'true',           'BOOLEAN',      '최대 실패 시 계정 잠금 활성화');

-- LOGIN_POLICY (7개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('LOGIN_POLICY', 'DUPLICATE_MAX',     '1',         'INTEGER', '중복 로그인 최대 허용 수(1=단일 세션)'),
    ('LOGIN_POLICY', 'DUPLICATE_ACTION',  'LOGOUT',    'ENUM',    '중복 로그인 처리(LOGOUT=기존 세션 종료|EXCEPTION=신규 차단)'),
    ('LOGIN_POLICY', 'HIST_KEEP_DAYS',    '365',       'INTEGER', '로그인 이력 보관 일수'),
    ('LOGIN_POLICY', 'ATTEMPT_LIMIT',     '0',         'INTEGER', '시간당 로그인 시도 제한(0=무제한)'),
    ('LOGIN_POLICY', 'IP_RESTRICTION',    'false',     'BOOLEAN', 'IP 제한 활성화'),
    ('LOGIN_POLICY', 'BLOCK_COUNTRIES',   '',          'MULTI_STRING', '접속 차단 국가(콤마 구분)'),
    ('LOGIN_POLICY', 'ANOMALY_DETECT',    'false',     'BOOLEAN', '이상 로그인 탐지 활성화');

-- ACCOUNT_POLICY (8개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('ACCOUNT_POLICY', 'ACCT_LOCK_PERIOD',        '0',     'INTEGER', '계정 잠금 기간(일, 0=무기한)'),
    ('ACCOUNT_POLICY', 'ACCT_LOCK_AUTO_RELEASE',  '0',     'INTEGER', '계정 잠금 자동 해제(분, 0=비활성)'),
    ('ACCOUNT_POLICY', 'ACCT_LOCK_ADMIN_ONLY',    'false', 'BOOLEAN', '잠금 해제 관리자만 가능'),
    ('ACCOUNT_POLICY', 'ACCT_STATUS_CHECK',       'true',  'BOOLEAN', '로그인 시 계정 상태 확인'),
    ('ACCOUNT_POLICY', 'ACCT_AUTH_METHOD',        'PASSWORD', 'ENUM', '인증 방식(PASSWORD|OTP|FIDO)'),
    ('ACCOUNT_POLICY', 'ACCT_OTP_ENABLED',        'false', 'BOOLEAN', 'OTP 인증 활성화'),
    ('ACCOUNT_POLICY', 'ACCT_FIDO_ENABLED',       'false', 'BOOLEAN', 'FIDO 인증 활성화'),
    ('ACCOUNT_POLICY', 'ACCT_DEVICE_AUTH',        'false', 'BOOLEAN', '디바이스 인증 활성화');

-- AUDIT_POLICY (5개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('AUDIT_POLICY', 'AUDIT_LOGIN_KEEP_DAYS',       '365', 'INTEGER', '로그인 이력 자동 삭제 주기(일)'),
    ('AUDIT_POLICY', 'AUDIT_PWD_CHANGE_KEEP_DAYS',  '365', 'INTEGER', '비밀번호 변경 이력 자동 삭제 주기(일)'),
    ('AUDIT_POLICY', 'AUDIT_PERM_CHANGE_KEEP_DAYS', '365', 'INTEGER', '권한 변경 이력 자동 삭제 주기(일)'),
    ('AUDIT_POLICY', 'AUDIT_ROLE_CHANGE_KEEP_DAYS', '365', 'INTEGER', '역할 변경 이력 자동 삭제 주기(일)'),
    ('AUDIT_POLICY', 'AUDIT_ACCT_KEEP_DAYS',        '365', 'INTEGER', '사용자 계정 이력 자동 삭제 주기(일)');

-- SYSTEM_POLICY (9개)
INSERT INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description) VALUES
    ('SYSTEM_POLICY', 'SESSION_TIMEOUT',      '30',     'INTEGER', '세션 타임아웃(분)'),
    ('SYSTEM_POLICY', 'SESSION_MAX_TIME',     '480',    'INTEGER', '세션 최대 유지 시간(분)'),
    ('SYSTEM_POLICY', 'API_RATE_LIMIT',       '0',      'INTEGER', 'API Rate Limit(초당 요청 수, 0=무제한)'),
    ('SYSTEM_POLICY', 'FILE_UPLOAD_ENABLED',  'true',   'BOOLEAN', '파일 업로드 허용'),
    ('SYSTEM_POLICY', 'FILE_ALLOWED_EXT',     'jpg,png,pdf,xlsx,docx', 'STRING', '허용 파일 확장자(콤마 구분)'),
    ('SYSTEM_POLICY', 'FILE_MAX_SIZE',        '10',     'INTEGER', '파일 최대 크기(MB)'),
    ('SYSTEM_POLICY', 'DOWNLOAD_ENABLED',     'true',   'BOOLEAN', '파일 다운로드 허용'),
    ('SYSTEM_POLICY', 'CORS_POLICY',          'SAME_ORIGIN', 'ENUM', 'CORS 정책(SAME_ORIGIN|ALLOW_ALL|CUSTOM)'),
    ('SYSTEM_POLICY', 'CSRF_ENABLED',              'true',  'BOOLEAN', 'CSRF 보호 활성화'),
    ('SYSTEM_POLICY', 'IP_ACCESS_CONTROL_ENABLED',  'false', 'BOOLEAN', 'IP 접근 제어 활성화 (등록 IP만 허용)'),
    ('SYSTEM_POLICY', 'MAC_ACCESS_CONTROL_ENABLED', 'false', 'BOOLEAN', 'MAC 접근 제어 활성화 (등록 MAC만 허용)');

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
