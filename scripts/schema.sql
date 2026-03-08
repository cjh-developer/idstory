-- ============================================================
--  IDStory 통합 로그인 시스템 - DDL 스크립트
--  파일 위치 : scripts/schema.sql
--  대상 DB   : MySQL 8.0+
--  문자셋   : utf8mb4 / utf8mb4_unicode_ci
--
--  ※ 테이블 명명 규칙 : ids_iam_{도메인}
-- ============================================================

CREATE DATABASE IF NOT EXISTS idstory_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE idstory_db;

-- ============================================================
--  DROP 순서 (FK 역순으로 제거)
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ids_iam_access_control_hist;
DROP TABLE IF EXISTS ids_iam_access_control;
DROP TABLE IF EXISTS ids_iam_login_hist;
DROP TABLE IF EXISTS ids_iam_user_acct_hist;
DROP TABLE IF EXISTS ids_iam_org_history;
DROP TABLE IF EXISTS ids_iam_comp_role;
DROP TABLE IF EXISTS ids_iam_grade;
DROP TABLE IF EXISTS ids_iam_position;
DROP TABLE IF EXISTS ids_iam_menu_role;
DROP TABLE IF EXISTS ids_iam_menu;
DROP TABLE IF EXISTS ids_iam_pwd_reset_token;
DROP TABLE IF EXISTS ids_iam_admin;
DROP TABLE IF EXISTS ids_iam_dept_head;
DROP TABLE IF EXISTS ids_iam_user_org_map;
DROP TABLE IF EXISTS ids_iam_user;
DROP TABLE IF EXISTS ids_iam_dept;
DROP TABLE IF EXISTS ids_iam_pwd_policy;
DROP TABLE IF EXISTS ids_iam_policy_hist;
DROP TABLE IF EXISTS ids_iam_policy;
DROP TABLE IF EXISTS ids_iam_role_subject;
DROP TABLE IF EXISTS ids_iam_role_user;
DROP TABLE IF EXISTS ids_iam_perm_subject;
DROP TABLE IF EXISTS ids_iam_perm_role;
DROP TABLE IF EXISTS ids_iam_permission;
DROP TABLE IF EXISTS ids_iam_role;
DROP TABLE IF EXISTS ids_iam_client;
DROP TABLE IF EXISTS users;           -- 기존 users 완전 제거
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  ids_iam_dept 테이블 (부서 정보)
-- ============================================================
CREATE TABLE ids_iam_dept (
    dept_oid         CHAR(18)        NOT NULL                         COMMENT '부서 OID (ids_+14자) — PK',
    dept_code        VARCHAR(20)     NOT NULL                         COMMENT '부서코드 (UNIQUE)',
    dept_name        VARCHAR(100)    NOT NULL                         COMMENT '부서명',
    parent_dept_oid  CHAR(18)        NULL                             COMMENT '상위 부서 OID (NULL=최상위)',
    sort_order       INT             NOT NULL    DEFAULT 0            COMMENT '정렬 순서',
    use_yn           CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '사용여부 Y|N',
    dept_type        VARCHAR(20)                                      COMMENT '부서 유형',
    dept_tel         VARCHAR(20)                                      COMMENT '부서 전화번호',
    dept_fax         VARCHAR(20)                                      COMMENT '부서 팩스번호',
    dept_address     VARCHAR(200)                                     COMMENT '부서 주소',
    created_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성 일시',
    created_by       VARCHAR(50)                                      COMMENT '생성자',
    updated_at       DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by       VARCHAR(50)                                      COMMENT '수정자',
    deleted_at       DATETIME                                         COMMENT '삭제 일시 (소프트 삭제)',
    deleted_by       VARCHAR(50)                                      COMMENT '삭제자',
    reserve_field_1  VARCHAR(255),
    reserve_field_2  VARCHAR(255),
    reserve_field_3  VARCHAR(255),
    reserve_field_4  VARCHAR(255),
    reserve_field_5  VARCHAR(255),

    CONSTRAINT pk_iam_dept           PRIMARY KEY (dept_oid),
    CONSTRAINT uq_iam_dept_code      UNIQUE KEY  (dept_code),
    CONSTRAINT fk_iam_dept_parent    FOREIGN KEY (parent_dept_oid)
                                         REFERENCES ids_iam_dept(dept_oid) ON DELETE SET NULL,
    INDEX idx_iam_dept_parent        (parent_dept_oid),
    INDEX idx_iam_dept_code          (dept_code),
    INDEX idx_iam_dept_use_yn        (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '부서 정보';

-- ============================================================
--  ids_iam_user 테이블 (시스템 사용자)
-- ============================================================
CREATE TABLE ids_iam_user (
    oid              CHAR(18)        NOT NULL                         COMMENT '사용자 OID (ids_+14자) — PK',
    user_id          VARCHAR(50)     NOT NULL                         COMMENT '로그인 계정',
    name             VARCHAR(100)    NOT NULL                         COMMENT '이름',
    password         VARCHAR(350)    NOT NULL                         COMMENT '비밀번호 해시 (순수 해시값)',
    password_salt    VARCHAR(100)                                     COMMENT '비밀번호 Salt (XML 설정값, 사용 시)',
    phone            VARCHAR(20)                                      COMMENT '휴대번호',
    email            VARCHAR(100)                                     COMMENT '이메일',
    dept_code        VARCHAR(20)                                      COMMENT '부서코드 (FK→ids_iam_dept)',
    role             VARCHAR(20)     NOT NULL    DEFAULT 'USER'       COMMENT '역할 USER|ADMIN',
    use_yn           CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '사용여부 Y|N',
    status           VARCHAR(10)     NOT NULL    DEFAULT 'ACTIVE'     COMMENT '상태 ACTIVE|SLEEPER|OUT',
    lock_yn          CHAR(1)         NOT NULL    DEFAULT 'N'          COMMENT '잠금여부 Y|N (비밀번호 연속 실패 초과 시 Y)',
    locked_at        DATETIME                                         COMMENT '잠금 일시 (자동 해제 기준)',
    login_fail_count INT             NOT NULL    DEFAULT 0            COMMENT '로그인 연속 실패 횟수',
    mfa_enabled_yn   CHAR(1)         NOT NULL    DEFAULT 'N'          COMMENT '2차 인증 Y|N',
    encrypt_yn       CHAR(1)         NOT NULL    DEFAULT 'N'          COMMENT 'PII 암호화 Y|N',
    concurrent_yn    CHAR(1)         NOT NULL    DEFAULT 'N'          COMMENT '겸직여부 Y|N',
    valid_start_date DATE                                             COMMENT '계정 사용 시작일',
    valid_end_date   DATE                                             COMMENT '계정 사용 종료일 (NULL=무제한)',
    created_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성일',
    created_by       VARCHAR(50)                                      COMMENT '생성자',
    updated_at       DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정일',
    updated_by       VARCHAR(50)                                      COMMENT '수정자',
    deleted_at       DATETIME                                         COMMENT '삭제일 (소프트 삭제)',
    deleted_by       VARCHAR(50)                                      COMMENT '삭제자',
    reserve_field_1  VARCHAR(255),
    reserve_field_2  VARCHAR(255),
    reserve_field_3  VARCHAR(255),
    reserve_field_4  VARCHAR(255),
    reserve_field_5  VARCHAR(255),

    CONSTRAINT pk_iam_user           PRIMARY KEY (oid),
    CONSTRAINT uq_iam_user_id        UNIQUE KEY (user_id),
    CONSTRAINT uq_iam_user_email     UNIQUE KEY (email),
    CONSTRAINT fk_iam_user_dept      FOREIGN KEY (dept_code)
                                         REFERENCES ids_iam_dept(dept_code) ON DELETE SET NULL,
    INDEX idx_iam_user_id            (user_id),
    INDEX idx_iam_user_status        (status),
    INDEX idx_iam_user_use_yn        (use_yn),
    INDEX idx_iam_user_lock_yn       (lock_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '시스템 사용자';

-- ============================================================
--  ids_iam_dept_head 테이블 (부서장 매핑)
-- ============================================================
CREATE TABLE ids_iam_dept_head (
    head_oid     CHAR(18)     NOT NULL                COMMENT '부서장 OID (PK)',
    dept_oid     CHAR(18)     NOT NULL                COMMENT '부서 OID',
    dept_name    VARCHAR(100)                         COMMENT '부서명 (비정규화)',
    user_oid     CHAR(18)     NOT NULL                COMMENT '사용자 OID',
    user_id      VARCHAR(50)                          COMMENT '사용자 아이디 (비정규화)',
    user_name    VARCHAR(100)                         COMMENT '사용자 이름 (비정규화)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    CONSTRAINT pk_iam_dept_head      PRIMARY KEY (head_oid),
    CONSTRAINT uq_iam_dept_head_dept UNIQUE KEY  (dept_oid),
    INDEX idx_iam_dept_head_user (user_oid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='부서장 매핑';

-- ============================================================
--  ids_iam_admin 테이블 (관리자 추가 정보)
-- ============================================================
CREATE TABLE ids_iam_admin (
    admin_oid    CHAR(18)        NOT NULL                         COMMENT '관리자 OID (ids_+14자) — PK',
    user_oid     CHAR(18)        NOT NULL                         COMMENT '사용자 OID (FK→ids_iam_user, UNIQUE)',
    admin_note   VARCHAR(500)                                     COMMENT '관리자 비고',
    granted_at   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '관리자 등록 일시',
    granted_by   VARCHAR(50)                                      COMMENT '등록자',
    updated_at   DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by   VARCHAR(50)                                      COMMENT '수정자',

    CONSTRAINT pk_iam_admin          PRIMARY KEY (admin_oid),
    CONSTRAINT uq_iam_admin_user     UNIQUE KEY (user_oid),
    CONSTRAINT fk_iam_admin_user     FOREIGN KEY (user_oid)
                                         REFERENCES ids_iam_user(oid) ON DELETE CASCADE,
    INDEX idx_iam_admin_user         (user_oid)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '관리자 추가 정보';

-- ============================================================
--  ids_iam_pwd_reset_token 테이블 (비밀번호 초기화 토큰)
-- ============================================================
CREATE TABLE ids_iam_pwd_reset_token (
    id          BIGINT          NOT NULL AUTO_INCREMENT              COMMENT '토큰 PK',
    token       VARCHAR(36)     NOT NULL                             COMMENT 'UUID 토큰',
    username    VARCHAR(50)     NOT NULL                             COMMENT '대상 사용자 아이디',
    expiry_date DATETIME        NOT NULL                             COMMENT '토큰 만료 일시',
    used        TINYINT(1)      NOT NULL    DEFAULT 0                COMMENT '사용 여부 (0: 미사용, 1: 사용됨)',
    created_at  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP               COMMENT '생성 일시',

    CONSTRAINT pk_iam_prt            PRIMARY KEY (id),
    CONSTRAINT uq_iam_prt_token      UNIQUE KEY (token),
    INDEX idx_iam_prt_username       (username),
    INDEX idx_iam_prt_expiry         (expiry_date)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '비밀번호 초기화 토큰';

-- ============================================================
--  ids_iam_menu 테이블 (시스템 메뉴)
-- ============================================================
CREATE TABLE ids_iam_menu (
    menu_id     BIGINT          NOT NULL AUTO_INCREMENT              COMMENT '메뉴 PK',
    parent_id   BIGINT          NULL                                 COMMENT '상위 메뉴 ID (NULL=최상위)',
    menu_name   VARCHAR(100)    NOT NULL                             COMMENT '메뉴명',
    icon        VARCHAR(100)    NULL                                 COMMENT 'Font Awesome 아이콘 클래스',
    url         VARCHAR(255)    NULL                                 COMMENT '링크 URL (NULL=폴더형)',
    sort_order  INT             NOT NULL    DEFAULT 0                COMMENT '정렬 순서',
    enabled     TINYINT(1)      NOT NULL    DEFAULT 1                COMMENT '활성화 여부',
    locked      TINYINT(1)      NOT NULL    DEFAULT 0                COMMENT '잠금 여부 (1=관리불가, 시스템 필수 메뉴)',
    created_at  DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP               COMMENT '생성 일시',
    updated_at  DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP             COMMENT '수정 일시',

    CONSTRAINT pk_iam_menu           PRIMARY KEY (menu_id),
    CONSTRAINT fk_iam_menu_parent    FOREIGN KEY (parent_id) REFERENCES ids_iam_menu(menu_id)
                                         ON DELETE CASCADE,
    INDEX idx_iam_menu_parent_id     (parent_id),
    INDEX idx_iam_menu_sort_order    (sort_order),
    INDEX idx_iam_menu_enabled       (enabled)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '시스템 메뉴';

-- ============================================================
--  ids_iam_menu_role 테이블 (메뉴-권한 매핑)
--  role이 없으면 모든 인증 사용자에게 표시
--  role이 있으면 해당 권한을 가진 사용자에게만 표시
-- ============================================================
CREATE TABLE ids_iam_menu_role (
    menu_id     BIGINT          NOT NULL    COMMENT '메뉴 FK',
    role        VARCHAR(20)     NOT NULL    COMMENT '권한 (USER | ADMIN)',

    CONSTRAINT pk_iam_menu_role      PRIMARY KEY (menu_id, role),
    CONSTRAINT fk_iam_menu_role_menu FOREIGN KEY (menu_id) REFERENCES ids_iam_menu(menu_id)
                                         ON DELETE CASCADE

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '메뉴-권한 매핑';

-- ============================================================
--  ids_iam_user_acct_hist 테이블 (사용자 계정 이력)
-- ============================================================
CREATE TABLE ids_iam_user_acct_hist (
    hist_oid        CHAR(18)        NOT NULL                         COMMENT '이력 OID',
    target_user_oid CHAR(18)        NOT NULL                         COMMENT '대상 사용자 OID',
    target_username VARCHAR(50)                                      COMMENT '대상 사용자 계정 (비정규화)',
    action_type     VARCHAR(30)     NOT NULL                         COMMENT 'CREATE|UPDATE|DELETE|LOCK|UNLOCK|RESET_PWD',
    action_detail   VARCHAR(500)                                     COMMENT '변경 상세',
    performed_by    VARCHAR(50)                                      COMMENT '처리자 username',
    performed_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '처리 일시',
    ip_address      VARCHAR(50)                                      COMMENT 'IP 주소',

    CONSTRAINT pk_iam_uah            PRIMARY KEY (hist_oid),
    INDEX idx_iam_uah_target         (target_user_oid),
    INDEX idx_iam_uah_performed_at   (performed_at),
    INDEX idx_iam_uah_action_type    (action_type)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '사용자 계정 이력';

-- ============================================================
--  ids_iam_login_hist 테이블 (로그인 이력)
-- ============================================================
CREATE TABLE ids_iam_login_hist (
    hist_oid     CHAR(18)        NOT NULL                         COMMENT '이력 OID — PK',
    user_oid     CHAR(18)                                         COMMENT '사용자 OID (참조용, FK 없음)',
    user_id      VARCHAR(50)     NOT NULL                         COMMENT '로그인 계정',
    action_type  VARCHAR(20)     NOT NULL                         COMMENT 'LOGIN_SUCCESS|LOGIN_FAIL|LOGOUT',
    fail_reason  VARCHAR(100)                                     COMMENT '실패 사유 (실패 시)',
    ip_address   VARCHAR(50)                                      COMMENT 'IP 주소',
    performed_at DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '처리 일시',

    CONSTRAINT pk_iam_login_hist     PRIMARY KEY (hist_oid),
    INDEX idx_iam_lh_user_id         (user_id),
    INDEX idx_iam_lh_performed_at    (performed_at),
    INDEX idx_iam_lh_action_type     (action_type)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '로그인 이력';

-- ============================================================
--  ids_iam_pwd_policy 테이블 (비밀번호 정책)
-- ============================================================
CREATE TABLE ids_iam_pwd_policy (
    policy_key   VARCHAR(50)     NOT NULL                         COMMENT '정책 키',
    policy_value VARCHAR(200)    NOT NULL                         COMMENT '정책 값',
    description  VARCHAR(200)                                     COMMENT '정책 설명',
    updated_at   DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    updated_by   VARCHAR(50)                                      COMMENT '수정자',

    CONSTRAINT pk_iam_pwd_policy     PRIMARY KEY (policy_key)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '비밀번호 정책';

-- ============================================================
--  ids_iam_position 테이블 (직위 관리)
-- ============================================================
CREATE TABLE ids_iam_position (
    position_oid  CHAR(18)        NOT NULL                         COMMENT '직위 OID (PK)',
    position_code VARCHAR(20)     NOT NULL                         COMMENT '직위 코드 (UNIQUE)',
    position_name VARCHAR(100)    NOT NULL                         COMMENT '직위명',
    position_desc VARCHAR(500)    NULL                             COMMENT '직위 상세 설명',
    sort_order    INT             NOT NULL    DEFAULT 0            COMMENT '정렬 순서',
    use_yn        CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '사용 여부 Y|N',
    created_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성 일시',
    created_by    VARCHAR(50)     NULL                             COMMENT '생성자',
    updated_at    DATETIME        NULL        DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by    VARCHAR(50)     NULL                             COMMENT '수정자',
    deleted_at    DATETIME        NULL                             COMMENT '삭제 일시 (소프트 삭제)',
    deleted_by    VARCHAR(50)     NULL                             COMMENT '삭제자',

    CONSTRAINT pk_iam_position      PRIMARY KEY (position_oid),
    CONSTRAINT uq_iam_position_code UNIQUE KEY  (position_code),
    INDEX idx_iam_position_code     (position_code),
    INDEX idx_iam_position_use_yn   (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '직위 관리';

-- ============================================================
--  ids_iam_grade 테이블 (직급 관리)
-- ============================================================
CREATE TABLE ids_iam_grade (
    grade_oid     CHAR(18)        NOT NULL                         COMMENT '직급 OID (PK)',
    grade_code    VARCHAR(20)     NOT NULL                         COMMENT '직급 코드 (UNIQUE)',
    grade_name    VARCHAR(100)    NOT NULL                         COMMENT '직급명',
    grade_desc    VARCHAR(500)    NULL                             COMMENT '직급 상세 설명',
    sort_order    INT             NOT NULL    DEFAULT 0            COMMENT '정렬 순서',
    use_yn        CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '사용 여부 Y|N',
    created_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성 일시',
    created_by    VARCHAR(50)     NULL                             COMMENT '생성자',
    updated_at    DATETIME        NULL        DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by    VARCHAR(50)     NULL                             COMMENT '수정자',
    deleted_at    DATETIME        NULL                             COMMENT '삭제 일시 (소프트 삭제)',
    deleted_by    VARCHAR(50)     NULL                             COMMENT '삭제자',

    CONSTRAINT pk_iam_grade         PRIMARY KEY (grade_oid),
    CONSTRAINT uq_iam_grade_code    UNIQUE KEY  (grade_code),
    INDEX idx_iam_grade_code        (grade_code),
    INDEX idx_iam_grade_use_yn      (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '직급 관리';

-- ============================================================
--  ids_iam_comp_role 테이블 (직책 관리)
-- ============================================================
CREATE TABLE ids_iam_comp_role (
    comp_role_oid  CHAR(18)        NOT NULL                         COMMENT '직책 OID (PK)',
    comp_role_code VARCHAR(20)     NOT NULL                         COMMENT '직책 코드 (UNIQUE)',
    comp_role_name VARCHAR(100)    NOT NULL                         COMMENT '직책명',
    comp_role_desc VARCHAR(500)    NULL                             COMMENT '직책 상세 설명',
    sort_order     INT             NOT NULL    DEFAULT 0            COMMENT '정렬 순서',
    use_yn         CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '사용 여부 Y|N',
    created_at     DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성 일시',
    created_by     VARCHAR(50)     NULL                             COMMENT '생성자',
    updated_at     DATETIME        NULL        DEFAULT CURRENT_TIMESTAMP
                                              ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by     VARCHAR(50)     NULL                             COMMENT '수정자',
    deleted_at     DATETIME        NULL                             COMMENT '삭제 일시 (소프트 삭제)',
    deleted_by     VARCHAR(50)     NULL                             COMMENT '삭제자',

    CONSTRAINT pk_iam_comp_role         PRIMARY KEY (comp_role_oid),
    CONSTRAINT uq_iam_comp_role_code    UNIQUE KEY  (comp_role_code),
    INDEX idx_iam_comp_role_code        (comp_role_code),
    INDEX idx_iam_comp_role_use_yn      (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '직책 관리';

-- ============================================================
--  ids_iam_user_org_map 테이블 (사용자-조직 매핑: 주소속/겸직)
-- ============================================================
CREATE TABLE ids_iam_user_org_map (
    map_oid          CHAR(18)        NOT NULL                         COMMENT '매핑 OID (PK)',
    user_oid         CHAR(18)        NOT NULL                         COMMENT '사용자 OID (참조)',
    user_id          VARCHAR(50)                                      COMMENT '사용자 아이디 (비정규화)',
    user_name        VARCHAR(100)                                     COMMENT '사용자 이름 (비정규화)',
    dept_oid         CHAR(18)                                         COMMENT '부서 OID (비정규화)',
    dept_name        VARCHAR(100)                                     COMMENT '부서명 (비정규화)',
    position_oid     CHAR(18)                                         COMMENT '직위 OID (비정규화)',
    position_name    VARCHAR(100)                                     COMMENT '직위명 (비정규화)',
    grade_oid        CHAR(18)                                         COMMENT '직급 OID (비정규화)',
    grade_name       VARCHAR(100)                                     COMMENT '직급명 (비정규화)',
    comp_role_oid    CHAR(18)                                         COMMENT '직책 OID (비정규화)',
    comp_role_name   VARCHAR(100)                                     COMMENT '직책명 (비정규화)',
    is_primary       CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '주소속 여부 Y=주소속 N=겸직',
    created_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '생성 일시',
    created_by       VARCHAR(50)                                      COMMENT '생성자',
    updated_at       DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by       VARCHAR(50)                                      COMMENT '수정자',

    CONSTRAINT pk_iam_uom            PRIMARY KEY (map_oid),
    INDEX idx_iam_uom_user_oid       (user_oid),
    INDEX idx_iam_uom_primary        (user_oid, is_primary)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '사용자-조직 매핑 (주소속/겸직)';

-- ============================================================
--  ids_iam_org_history 테이블 (조직 이력 - 직위/직급 통합)
-- ============================================================
CREATE TABLE ids_iam_org_history (
    history_oid  CHAR(18)        NOT NULL                         COMMENT '이력 OID (PK)',
    target_type  VARCHAR(20)     NOT NULL                         COMMENT '대상 구분 POSITION|GRADE|COMP_ROLE',
    target_oid   CHAR(18)        NOT NULL                         COMMENT '대상 OID',
    action_type  VARCHAR(20)     NOT NULL                         COMMENT '처리 구분 CREATE|UPDATE|DELETE',
    before_data  TEXT            NULL                             COMMENT '변경 전 데이터 (JSON)',
    after_data   TEXT            NULL                             COMMENT '변경 후 데이터 (JSON)',
    action_at    DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '처리 일시',
    action_by    VARCHAR(50)     NULL                             COMMENT '처리자',

    CONSTRAINT pk_iam_org_hist      PRIMARY KEY (history_oid),
    INDEX idx_iam_org_hist_target   (target_type, target_oid),
    INDEX idx_iam_org_hist_at       (action_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '조직 이력 (직위/직급/직책 통합)';

-- ============================================================
--  ids_iam_client 테이블 (클라이언트/시스템/서비스)
-- ============================================================
CREATE TABLE ids_iam_client (
    client_oid   CHAR(18)     NOT NULL  COMMENT '클라이언트 OID (PK)',
    client_code  VARCHAR(50)  NOT NULL  COMMENT '클라이언트 코드 (UNIQUE)',
    client_name  VARCHAR(100) NOT NULL  COMMENT '클라이언트명',
    parent_oid   CHAR(18)     NULL      COMMENT '상위 클라이언트 OID (NULL=최상위)',
    description  VARCHAR(500) NULL,
    app_type     VARCHAR(10)  NOT NULL  DEFAULT 'IAM'  COMMENT 'APP 유형: SSO|IAM|EAM|IEAM',
    sort_order   INT          NOT NULL  DEFAULT 0,
    use_yn       CHAR(1)      NOT NULL  DEFAULT 'Y',
    created_at   DATETIME     NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted_at   DATETIME     NULL,
    deleted_by   VARCHAR(50),
    CONSTRAINT pk_iam_client      PRIMARY KEY (client_oid),
    CONSTRAINT uq_iam_client_code UNIQUE KEY  (client_code),
    CONSTRAINT fk_iam_client_par  FOREIGN KEY (parent_oid) REFERENCES ids_iam_client(client_oid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='클라이언트(시스템/서비스)';

-- ============================================================
--  ids_iam_role 테이블 (역할)
-- ============================================================
CREATE TABLE ids_iam_role (
    role_oid     CHAR(18)     NOT NULL  COMMENT '역할 OID (PK)',
    role_code    VARCHAR(50)  NOT NULL  COMMENT '역할 코드 (UNIQUE)',
    role_name    VARCHAR(100) NOT NULL  COMMENT '역할명',
    parent_oid   CHAR(18)     NULL      COMMENT '상위 역할 OID',
    description  VARCHAR(500) NULL,
    sort_order   INT          NOT NULL  DEFAULT 0,
    use_yn       CHAR(1)      NOT NULL  DEFAULT 'Y',
    created_at   DATETIME     NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted_at   DATETIME     NULL,
    deleted_by   VARCHAR(50),
    CONSTRAINT pk_iam_role      PRIMARY KEY (role_oid),
    CONSTRAINT uq_iam_role_code UNIQUE KEY  (role_code),
    CONSTRAINT fk_iam_role_par  FOREIGN KEY (parent_oid) REFERENCES ids_iam_role(role_oid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할';

-- ============================================================
--  ids_iam_permission 테이블 (권한)
-- ============================================================
CREATE TABLE ids_iam_permission (
    perm_oid     CHAR(18)     NOT NULL  COMMENT '권한 OID (PK)',
    client_oid   CHAR(18)     NOT NULL  COMMENT '클라이언트 OID (FK)',
    perm_code    VARCHAR(100) NOT NULL  COMMENT '권한 코드 (UNIQUE)',
    perm_name    VARCHAR(100) NOT NULL  COMMENT '권한명',
    parent_oid   CHAR(18)     NULL      COMMENT '상위 권한 OID',
    description  VARCHAR(500) NULL,
    sort_order   INT          NOT NULL  DEFAULT 0,
    use_yn       CHAR(1)      NOT NULL  DEFAULT 'Y',
    created_at   DATETIME     NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    created_by   VARCHAR(50),
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by   VARCHAR(50),
    deleted_at   DATETIME     NULL,
    deleted_by   VARCHAR(50),
    CONSTRAINT pk_iam_perm        PRIMARY KEY (perm_oid),
    CONSTRAINT uq_iam_perm_code   UNIQUE KEY  (perm_code),
    CONSTRAINT fk_iam_perm_client FOREIGN KEY (client_oid) REFERENCES ids_iam_client(client_oid),
    CONSTRAINT fk_iam_perm_parent FOREIGN KEY (parent_oid) REFERENCES ids_iam_permission(perm_oid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='권한';

-- ============================================================
--  ids_iam_perm_role 테이블 (권한-역할 매핑)
-- ============================================================
CREATE TABLE ids_iam_perm_role (
    perm_role_oid CHAR(18) NOT NULL  COMMENT '매핑 OID (PK)',
    perm_oid      CHAR(18) NOT NULL  COMMENT '권한 OID (FK)',
    role_oid      CHAR(18) NOT NULL  COMMENT '역할 OID (FK)',
    created_at    DATETIME NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50),
    CONSTRAINT pk_iam_perm_role  PRIMARY KEY (perm_role_oid),
    CONSTRAINT uq_iam_perm_role  UNIQUE KEY  (perm_oid, role_oid),
    CONSTRAINT fk_iam_pr_perm    FOREIGN KEY (perm_oid) REFERENCES ids_iam_permission(perm_oid) ON DELETE CASCADE,
    CONSTRAINT fk_iam_pr_role    FOREIGN KEY (role_oid) REFERENCES ids_iam_role(role_oid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='권한-역할 매핑';

-- ============================================================
--  ids_iam_role_user 테이블 (역할-사용자 매핑)
-- ============================================================
CREATE TABLE ids_iam_role_user (
    role_user_oid CHAR(18)    NOT NULL COMMENT '매핑 OID (PK)',
    role_oid      CHAR(18)    NOT NULL COMMENT '역할 OID (FK → ids_iam_role)',
    user_oid      CHAR(18)    NOT NULL COMMENT '사용자 OID (FK → ids_iam_user)',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(50) NULL,
    CONSTRAINT pk_iam_role_user  PRIMARY KEY (role_user_oid),
    CONSTRAINT uq_iam_role_user  UNIQUE KEY  (role_oid, user_oid),
    CONSTRAINT fk_ru_role        FOREIGN KEY (role_oid) REFERENCES ids_iam_role(role_oid) ON DELETE CASCADE,
    CONSTRAINT fk_ru_user        FOREIGN KEY (user_oid) REFERENCES ids_iam_user(oid)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할-사용자 매핑';

-- ============================================================
--  ids_iam_role_subject 테이블 (역할 대상: 부서/직위/직급/예외)
-- ============================================================
CREATE TABLE ids_iam_role_subject (
    role_subject_oid CHAR(18)    NOT NULL COMMENT '매핑 OID (PK)',
    role_oid         CHAR(18)    NOT NULL COMMENT '역할 OID (FK → ids_iam_role)',
    subject_type     VARCHAR(20) NOT NULL COMMENT '대상 유형: DEPT|POSITION|GRADE|EXCEPTION',
    subject_oid      CHAR(18)    NOT NULL COMMENT '대상 OID (dept_oid|position_oid|grade_oid|user_oid)',
    include_children CHAR(1)     NOT NULL DEFAULT 'N' COMMENT '하위 부서 포함 여부 (DEPT 전용)',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50) NULL,
    CONSTRAINT pk_iam_role_subject PRIMARY KEY (role_subject_oid),
    CONSTRAINT uq_iam_role_subject UNIQUE KEY  (role_oid, subject_type, subject_oid),
    CONSTRAINT fk_rsubj_role       FOREIGN KEY (role_oid) REFERENCES ids_iam_role(role_oid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='역할 대상 매핑(부서/직위/직급/예외)';

-- ============================================================
--  ids_iam_perm_subject 테이블 (권한 대상: 부서/개인/직급/직위/예외)
-- ============================================================
CREATE TABLE ids_iam_perm_subject (
    perm_subject_oid CHAR(18)    NOT NULL COMMENT '매핑 OID (PK)',
    perm_oid         CHAR(18)    NOT NULL COMMENT '권한 OID (FK → ids_iam_permission)',
    subject_type     VARCHAR(20) NOT NULL COMMENT '대상 유형: DEPT|USER|GRADE|POSITION|EXCEPTION',
    subject_oid      CHAR(18)    NOT NULL COMMENT '대상 OID (dept_oid | user oid | grade_oid | position_oid)',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(50) NULL,
    CONSTRAINT pk_iam_perm_subject PRIMARY KEY (perm_subject_oid),
    CONSTRAINT uq_iam_perm_subject UNIQUE KEY  (perm_oid, subject_type, subject_oid),
    CONSTRAINT fk_ps_perm          FOREIGN KEY (perm_oid) REFERENCES ids_iam_permission(perm_oid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='권한 대상 (부서/개인/직급/직위/예외사용자)';

-- ============================================================
--  ids_iam_policy 테이블 (통합 정책 Key-Value)
-- ============================================================
CREATE TABLE ids_iam_policy (
    policy_group VARCHAR(30)     NOT NULL                         COMMENT '정책 그룹 (ADMIN_POLICY|USER_POLICY|PASSWORD_POLICY|LOGIN_POLICY|ACCOUNT_POLICY|AUDIT_POLICY|SYSTEM_POLICY)',
    policy_key   VARCHAR(60)     NOT NULL                         COMMENT '정책 키',
    policy_value VARCHAR(500)    NOT NULL                         COMMENT '정책 값',
    value_type   VARCHAR(20)     NOT NULL    DEFAULT 'STRING'     COMMENT '값 유형 (STRING|INTEGER|BOOLEAN|ENUM|MULTI_STRING)',
    description  VARCHAR(200)                                     COMMENT '정책 설명',
    updated_at   DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    updated_by   VARCHAR(50)                                      COMMENT '수정자',

    CONSTRAINT pk_iam_policy PRIMARY KEY (policy_group, policy_key),
    INDEX idx_iam_policy_group (policy_group)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '통합 정책 (Key-Value)';

-- ============================================================
--  ids_iam_policy_hist 테이블 (정책 변경 이력)
-- ============================================================
CREATE TABLE ids_iam_policy_hist (
    hist_oid     CHAR(18)        NOT NULL                         COMMENT '이력 OID (PK)',
    policy_group VARCHAR(30)     NOT NULL                         COMMENT '정책 그룹',
    policy_key   VARCHAR(60)     NOT NULL                         COMMENT '정책 키',
    old_value    VARCHAR(500)                                     COMMENT '변경 전 값',
    new_value    VARCHAR(500)                                     COMMENT '변경 후 값',
    changed_at   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '변경 일시',
    changed_by   VARCHAR(50)                                      COMMENT '변경자',

    CONSTRAINT pk_iam_policy_hist    PRIMARY KEY (hist_oid),
    INDEX idx_iam_ph_group_key       (policy_group, policy_key),
    INDEX idx_iam_ph_changed_at      (changed_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '정책 변경 이력';

-- ============================================================
--  ids_iam_access_control 테이블 (IP/MAC 접근 제어 규칙)
-- ============================================================
CREATE TABLE ids_iam_access_control (
    rule_oid     CHAR(18)        NOT NULL                         COMMENT '규칙 OID (PK)',
    control_type VARCHAR(10)     NOT NULL                         COMMENT '제어 유형 IP|MAC',
    rule_value   VARCHAR(50)     NOT NULL                         COMMENT 'IP/CIDR 또는 MAC 주소',
    ip_version   VARCHAR(4)      NULL                             COMMENT 'IP 전용: IPV4|IPV6 (MAC은 NULL)',
    description  VARCHAR(200)    NULL                             COMMENT '설명',
    use_yn       CHAR(1)         NOT NULL    DEFAULT 'Y'          COMMENT '규칙 활성화 여부 Y|N',
    created_at   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '등록 일시',
    created_by   VARCHAR(50)                                      COMMENT '등록자',
    updated_at   DATETIME                    DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP         COMMENT '수정 일시',
    updated_by   VARCHAR(50)                                      COMMENT '수정자',

    CONSTRAINT pk_iam_access_ctrl   PRIMARY KEY (rule_oid),
    CONSTRAINT uq_iam_access_ctrl   UNIQUE KEY  (control_type, rule_value),
    INDEX idx_iam_ac_type           (control_type),
    INDEX idx_iam_ac_use_yn         (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'IP/MAC 접근 제어 규칙';

-- ============================================================
--  ids_iam_access_control_hist 테이블 (접근 차단 이력)
-- ============================================================
CREATE TABLE ids_iam_access_control_hist (
    hist_oid     CHAR(18)        NOT NULL                         COMMENT '이력 OID (PK)',
    control_type VARCHAR(10)     NOT NULL                         COMMENT '차단 유형 IP|MAC',
    request_val  VARCHAR(100)    NOT NULL                         COMMENT '차단된 접속 IP 또는 MAC',
    request_uri  VARCHAR(255)    NULL                             COMMENT '요청 URI',
    blocked_at   DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP           COMMENT '차단 일시',

    CONSTRAINT pk_iam_ac_hist    PRIMARY KEY (hist_oid),
    INDEX idx_iam_ach_type       (control_type),
    INDEX idx_iam_ach_blocked_at (blocked_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '접근 제어 차단 이력';

-- ============================================================
--  SSO (OIDC) 관련 테이블
-- ============================================================

-- ── ids_iam_sso_key_pair (RSA 서명 키 쌍) ──────────────────
CREATE TABLE ids_iam_sso_key_pair (
    key_id          VARCHAR(36)     NOT NULL                            COMMENT 'Key UUID (PK)',
    private_key_pem TEXT            NOT NULL                            COMMENT 'RSA 개인키 PEM',
    public_key_pem  TEXT            NOT NULL                            COMMENT 'RSA 공개키 PEM',
    active_yn       CHAR(1)         NOT NULL    DEFAULT 'Y'             COMMENT '활성 여부 Y|N',
    created_at      DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',

    CONSTRAINT pk_iam_sso_kp    PRIMARY KEY (key_id),
    INDEX idx_iam_sso_kp_active (active_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SSO RSA 서명 키 쌍';

-- ── ids_iam_sso_client (SSO 클라이언트 설정) ────────────────
CREATE TABLE ids_iam_sso_client (
    sso_client_oid              CHAR(18)        NOT NULL                            COMMENT 'SSO 클라이언트 OID (PK)',
    client_oid                  CHAR(18)        NOT NULL                            COMMENT '클라이언트 OID (FK→ids_iam_client)',
    client_id                   VARCHAR(100)    NOT NULL                            COMMENT 'OIDC client_id (unique)',
    enc_client_secret           VARCHAR(64)     NOT NULL                            COMMENT 'SHA-256 HEX 해시된 시크릿',
    redirect_uris               TEXT            NOT NULL                            COMMENT '허용 redirect URI (줄바꿈 구분)',
    scopes                      VARCHAR(500)    NOT NULL    DEFAULT 'openid profile email' COMMENT '허용 스코프 (공백 구분)',
    grant_types                 VARCHAR(200)    NOT NULL    DEFAULT 'authorization_code'   COMMENT '허용 grant type',
    access_token_validity_sec   INT             NOT NULL    DEFAULT 3600            COMMENT 'Access Token 유효시간(초)',
    refresh_token_validity_sec  INT             NOT NULL    DEFAULT 86400           COMMENT 'Refresh Token 유효시간(초)',
    id_token_validity_sec       INT             NOT NULL    DEFAULT 3600            COMMENT 'ID Token 유효시간(초)',
    use_yn                      CHAR(1)         NOT NULL    DEFAULT 'Y'             COMMENT '사용여부 Y|N',
    auth_uri                    VARCHAR(500)    NULL                                COMMENT 'SSO 세션 없을 때 이동하는 인증 페이지',
    auth_result                 VARCHAR(500)    NULL                                COMMENT '인증 완료 후 AUTH_CODE 콜백 주소',
    no_use_sso                  TEXT            NULL                                COMMENT 'SSO 미사용 URI 목록 (줄바꿈 구분)',
    created_at                  DATETIME        DEFAULT CURRENT_TIMESTAMP           COMMENT '생성일시',
    created_by                  VARCHAR(50)                                         COMMENT '생성자',
    updated_at                  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    updated_by                  VARCHAR(50)                                         COMMENT '수정자',

    CONSTRAINT pk_iam_sso_client        PRIMARY KEY (sso_client_oid),
    CONSTRAINT uq_iam_sso_client_oid    UNIQUE KEY  (client_oid),
    CONSTRAINT uq_iam_sso_client_id     UNIQUE KEY  (client_id),
    CONSTRAINT fk_iam_sso_client_oid    FOREIGN KEY (client_oid) REFERENCES ids_iam_client(client_oid) ON DELETE CASCADE,
    INDEX idx_iam_sso_c_use_yn          (use_yn)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SSO OIDC 클라이언트 설정';

-- ── ids_iam_sso_auth_code (인증코드 발행 이력) ──────────────
CREATE TABLE ids_iam_sso_auth_code (
    code_oid        CHAR(18)        NOT NULL                            COMMENT '이력 OID (PK)',
    auth_code       VARCHAR(128)    NOT NULL                            COMMENT '발행된 인증코드',
    client_oid      CHAR(18)        NOT NULL                            COMMENT '클라이언트 OID',
    client_id       VARCHAR(100)    NOT NULL                            COMMENT 'OIDC client_id',
    user_oid        CHAR(18)        NOT NULL                            COMMENT '사용자 OID',
    user_id         VARCHAR(50)     NOT NULL                            COMMENT '사용자 계정',
    redirect_uri    VARCHAR(1000)   NOT NULL                            COMMENT '요청 redirect URI',
    scopes          VARCHAR(200)                                        COMMENT '요청 스코프',
    state           VARCHAR(500)                                        COMMENT 'OAuth2 state',
    issued_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '발행일시',
    expires_at      DATETIME        NOT NULL                            COMMENT '만료일시',
    used_at         DATETIME        NULL                                COMMENT '사용일시',
    status          VARCHAR(10)     NOT NULL    DEFAULT 'ISSUED'        COMMENT '상태 ISSUED|USED|EXPIRED',
    ip_address      VARCHAR(100)                                        COMMENT '클라이언트 IP',

    CONSTRAINT pk_iam_sso_ac        PRIMARY KEY (code_oid),
    CONSTRAINT uq_iam_sso_ac_code   UNIQUE KEY  (auth_code),
    INDEX idx_iam_sso_ac_client     (client_oid),
    INDEX idx_iam_sso_ac_user       (user_oid),
    INDEX idx_iam_sso_ac_status     (status),
    INDEX idx_iam_sso_ac_issued     (issued_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SSO 인증코드(Authorization Code) 발행 이력';

-- ── ids_iam_sso_token (발행 토큰 이력) ──────────────────────
CREATE TABLE ids_iam_sso_token (
    token_oid       CHAR(18)        NOT NULL                            COMMENT '토큰 OID (PK)',
    client_oid      CHAR(18)        NOT NULL                            COMMENT '클라이언트 OID',
    client_id       VARCHAR(100)    NOT NULL                            COMMENT 'OIDC client_id',
    user_oid        CHAR(18)        NOT NULL                            COMMENT '사용자 OID',
    user_id         VARCHAR(50)     NOT NULL                            COMMENT '사용자 계정',
    token_type      VARCHAR(10)     NOT NULL                            COMMENT '토큰 유형 ACCESS|REFRESH|ID',
    jti             VARCHAR(100)    NOT NULL                            COMMENT 'JWT ID (UUID)',
    scopes          VARCHAR(200)                                        COMMENT '발행 스코프',
    issued_at       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '발행일시',
    expires_at      DATETIME        NOT NULL                            COMMENT '만료일시',
    revoked_at      DATETIME        NULL                                COMMENT '취소일시',
    ip_address      VARCHAR(100)                                        COMMENT '클라이언트 IP',

    CONSTRAINT pk_iam_sso_token     PRIMARY KEY (token_oid),
    CONSTRAINT uq_iam_sso_token_jti UNIQUE KEY  (jti),
    INDEX idx_iam_sso_tk_client     (client_oid),
    INDEX idx_iam_sso_tk_user       (user_oid),
    INDEX idx_iam_sso_tk_type       (token_type),
    INDEX idx_iam_sso_tk_issued     (issued_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SSO 발행 토큰 이력';

-- ── ids_iam_sso_access_log (SSO 접근 이력) ──────────────────
CREATE TABLE ids_iam_sso_access_log (
    log_oid         CHAR(18)        NOT NULL                            COMMENT '이력 OID (PK)',
    client_oid      CHAR(18)                                            COMMENT '클라이언트 OID',
    client_id       VARCHAR(100)                                        COMMENT 'OIDC client_id',
    user_oid        CHAR(18)                                            COMMENT '사용자 OID',
    user_id         VARCHAR(50)                                         COMMENT '사용자 계정',
    ip_address      VARCHAR(100)                                        COMMENT '접속 IP',
    action          VARCHAR(20)     NOT NULL                            COMMENT '행위 AUTHORIZE|TOKEN|USERINFO|LOGOUT|DISCOVERY|JWKS',
    status          VARCHAR(10)     NOT NULL                            COMMENT '결과 SUCCESS|FAIL|DENIED',
    detail          VARCHAR(500)                                        COMMENT '상세 내용',
    accessed_at     DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '접근일시',

    CONSTRAINT pk_iam_sso_al        PRIMARY KEY (log_oid),
    INDEX idx_iam_sso_al_client     (client_id),
    INDEX idx_iam_sso_al_user       (user_id),
    INDEX idx_iam_sso_al_action     (action),
    INDEX idx_iam_sso_al_accessed   (accessed_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'SSO 접근 이력';
