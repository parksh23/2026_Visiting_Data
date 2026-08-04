-- 실행 전 아래 조회 결과가 없어야 한다.
-- SELECT NICKNAME, COUNT(*) FROM APP_USERS
-- GROUP BY NICKNAME HAVING COUNT(*) > 1;
--
-- 운영 DB에는 현재 'test' 닉네임 중복이 있어 먼저 해당 테스트 계정을
-- 정리한 뒤 이 제약을 적용해야 한다.
ALTER TABLE APP_USERS ADD CONSTRAINT UQ_APP_USERS_NICKNAME UNIQUE (NICKNAME);
