-- ============================================================
--  IDStory 마이그레이션 스크립트
--  날짜: 2026-03-09
--  내용:
--    1. SSO 로그인 체크 Access Token 연장 시간 정책 추가
--       (SYSTEM_POLICY.SSO_ACCESS_TOKEN_EXTEND_SEC)
--
--  실행 방법: MySQL에서 직접 실행하거나 schema.sql + data.sql 재실행
-- ============================================================

-- 1. SSO Access Token 연장 시간 정책 추가
INSERT IGNORE INTO ids_iam_policy (policy_group, policy_key, policy_value, value_type, description)
VALUES ('SYSTEM_POLICY', 'SSO_ACCESS_TOKEN_EXTEND_SEC', '1800', 'INTEGER',
        'SSO 로그인 체크(/sso/check) 시 Access Token 연장 시간(초)');
