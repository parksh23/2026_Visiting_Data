from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status, Depends
from fastapi.security import OAuth2PasswordBearer
from jose import JWTError, jwt
from passlib.context import CryptContext


# 개발용 비밀키
SECRET_KEY = "busan-quest-dev-secret-key"

# JWT 서명 알고리즘
ALGORITHM = "HS256"

# 토큰 만료 시간
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24  # 24시간


# ==========================================
# 🌟 추가된 부분: Swagger UI 자물쇠 버튼 활성화 및 토큰 자동 추출
# tokenUrl에는 로그인 API의 실제 경로를 적어줍니다.
# ==========================================
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


# 로그인/회원가입 성공 시 JWT를 생성하는 함수
def create_access_token(data: dict):
    # JWT payload에 넣을 데이터 복사
    to_encode = data.copy()

    # 만료 시간 추가
    expire = datetime.now(timezone.utc) + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})

    # JWT 문자열 생성
    encoded_jwt = jwt.encode(
        to_encode,
        SECRET_KEY,
        algorithm=ALGORITHM
    )

    return encoded_jwt


# ==========================================
# 🌟 수정된 부분: Header 직접 참조 대신 Depends(oauth2_scheme) 사용
# ==========================================
def get_current_user_email(token: str = Depends(oauth2_scheme)):
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="인증 정보가 올바르지 않습니다.",
        headers={"WWW-Authenticate": "Bearer"},
    )

    # OAuth2PasswordBearer가 "Bearer "를 자동으로 제거하고 순수 토큰 문자열만 token 변수에 담아줍니다.
    try:
        payload = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=[ALGORITHM]
        )

        email: str | None = payload.get("sub")

        if email is None:
            raise credentials_exception

        return email

    except JWTError:
        raise credentials_exception


pwd_context = CryptContext(
    schemes=["bcrypt"],
    deprecated="auto"
)


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, password_hash: str) -> bool:
    return pwd_context.verify(plain_password, password_hash)