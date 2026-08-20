# Busan Quest Backend

FastAPI와 Oracle SQLAlchemy로 인증, 사용자, 미션, 찜, 인증, 랭킹 API를 제공합니다. 실행 및 환경 설정은 저장소 루트의 `README.md`를 따릅니다.

## 주요 환경변수

- `DATABASE_URL` 또는 `ORACLE_USER`, `ORACLE_PASSWORD`, `ORACLE_DSN`
- `JWT_SECRET_KEY`: 필수. 없으면 서버가 시작되지 않습니다.
- `GEMINI_API_KEY`: 사진·영수증 인증
- `TOURISM_DATA_SERVICE_KEY`, `TOURISM_ADMIN_KEY`: 관광점수 갱신
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`: 비밀번호 복구 메일
- `CORS_ALLOWED_ORIGINS`: 웹 클라이언트가 있을 때만 쉼표로 구분해 설정

`app/.env.example`을 복사해 로컬 `.env`를 만들고 실제 비밀정보는 Git에 커밋하지 않습니다.
