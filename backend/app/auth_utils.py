import os
from datetime import datetime, timedelta, timezone

from fastapi import Header, HTTPException, status
from jose import JWTError, jwt
from passlib.context import CryptContext

SECRET_KEY = os.getenv("JWT_SECRET_KEY", "change-this-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("JWT_EXPIRE_MINUTES", str(60 * 24 * 7)))

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def create_access_token(data: dict) -> str:
    payload = data.copy()
    payload["exp"] = datetime.now(timezone.utc) + timedelta(
        minutes=ACCESS_TOKEN_EXPIRE_MINUTES
    )
    return jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)


def get_current_user_email(authorization: str | None = Header(default=None)) -> str:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="로그인이 필요하거나 인증 정보가 만료되었습니다.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    if not authorization or not authorization.startswith("Bearer "):
        raise credentials_exception

    token = authorization.removeprefix("Bearer ").strip()
    try:
        subject = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM]).get("sub")
        if not subject:
            raise credentials_exception
        return str(subject)
    except JWTError as exc:
        raise credentials_exception from exc


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, password_hash: str | None) -> bool:
    return bool(password_hash) and pwd_context.verify(plain_password, password_hash)
