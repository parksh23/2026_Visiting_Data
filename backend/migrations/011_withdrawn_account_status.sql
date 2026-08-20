-- 회원 탈퇴를 하드 삭제 → 소프트 삭제로 바꾸면서 ACCOUNT_STATUS 에 'WITHDRAWN' 값이 새로 생겼다.
--
-- 하드 삭제를 못 쓰는 이유:
--   USER_AGREEMENTS(008) 가 APP_USERS.USER_CODE 를 FK 로 잡고 있어서,
--   약관 동의 이력이 있는 계정은 DELETE 가 FK 위반으로 실패한다.
--   동의 이력은 "언제 어떤 버전에 동의했는가"를 남겨야 하는 증빙이라 같이 지울 수도 없다.
--
-- 대신 API 가 탈퇴 시 EMAIL / KAKAO_ID / PASSWORD_HASH 를 NULL 로 지우고
-- LOGIN_ID / NICKNAME 을 'withdrawn:USER_CODE' / '탈퇴한사용자_USER_CODE' 로 덮어써서
-- 남는 값이 USER_CODE 와 동의 이력뿐이 되게 한다.

-- 1) ACCOUNT_STATUS 에 값을 제한하는 CHECK 제약이 걸려 있는지 먼저 확인한다.
--    결과가 나오고 SEARCH_CONDITION 에 'WITHDRAWN' 이 없다면 아래 2)를 실행한다.
--    결과가 비어 있으면 이 마이그레이션은 실행할 것이 없다(문서 역할만 한다).
--
-- SELECT CONSTRAINT_NAME, SEARCH_CONDITION
--   FROM USER_CONSTRAINTS
--  WHERE TABLE_NAME = 'APP_USERS' AND CONSTRAINT_TYPE = 'C';

-- 2) 제약이 있고 'WITHDRAWN' 을 허용하지 않는 경우에만 실행:
--    (아래 CK_APP_USERS_STATUS 자리에 1)에서 찾은 실제 제약 이름을 넣을 것)
--
-- ALTER TABLE APP_USERS DROP CONSTRAINT CK_APP_USERS_STATUS;
-- ALTER TABLE APP_USERS ADD CONSTRAINT CK_APP_USERS_STATUS
--     CHECK (ACCOUNT_STATUS IN ('ACTIVE','SUSPENDED','WITHDRAWN'));

-- 3) 탈퇴 계정은 랭킹·친구 조회에서 이미 ACCOUNT_STATUS = 'ACTIVE' 필터로 빠지지만,
--    조회가 잦은 컬럼이라 인덱스를 둔다. (이미 있으면 ORA-01408 이 나며 무시해도 된다)
CREATE INDEX IX_APP_USERS_ACCOUNT_STATUS ON APP_USERS(ACCOUNT_STATUS);

COMMIT;
