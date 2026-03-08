-- ============================================================
--  IDStory 마이그레이션 스크립트
--  날짜: 2026-03-08
--  내용:
--    1. ids_iam_client에 app_type 컬럼 추가
--    2. ids_iam_sso_client에 auth_uri / auth_result / no_use_sso 컬럼 추가
--    3. # 플레이스홀더 메뉴 비활성화 (14개)
--
--  실행 방법: MySQL에서 직접 실행하거나 schema.sql + data.sql 재실행
-- ============================================================

USE idstory;

-- ─────────────────────────────────────────────────────────────
-- 1. ids_iam_client: app_type 컬럼 추가
-- ─────────────────────────────────────────────────────────────
ALTER TABLE ids_iam_client
    ADD COLUMN IF NOT EXISTS app_type VARCHAR(10) NOT NULL DEFAULT 'IAM'
    COMMENT 'APP 유형: SSO|IAM|EAM|IEAM'
    AFTER description;

-- ─────────────────────────────────────────────────────────────
-- 2. ids_iam_sso_client: auth_uri / auth_result / no_use_sso 추가
--    (테이블이 없는 경우 schema.sql 재실행 필요)
-- ─────────────────────────────────────────────────────────────
ALTER TABLE ids_iam_sso_client
    ADD COLUMN IF NOT EXISTS auth_uri    VARCHAR(500) NULL COMMENT 'SSO 세션 없을 때 이동하는 인증 페이지' AFTER use_yn,
    ADD COLUMN IF NOT EXISTS auth_result VARCHAR(500) NULL COMMENT '인증 완료 후 AUTH_CODE 콜백 주소'      AFTER auth_uri,
    ADD COLUMN IF NOT EXISTS no_use_sso  TEXT         NULL COMMENT 'SSO 미사용 URI 목록 (줄바꿈 구분)'     AFTER auth_result;

-- ─────────────────────────────────────────────────────────────
-- 3. # 플레이스홀더 메뉴 비활성화 (14개)
--    url='#' 인 모든 enabled 메뉴를 비활성화
-- ─────────────────────────────────────────────────────────────
UPDATE ids_iam_menu SET enabled = 0 WHERE url = '#' AND enabled = 1;
