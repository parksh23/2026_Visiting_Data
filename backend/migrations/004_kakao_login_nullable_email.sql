-- 카카오 계정은 이메일 제공 동의를 하지 않을 수 있으므로 NULL을 허용한다.
-- LOGIN_ID는 kakao:<카카오 사용자 ID> 형식으로 항상 저장한다.
ALTER TABLE APP_USERS MODIFY (EMAIL NULL);
