import logging
from pathlib import Path

from sqlalchemy import func
from sqlalchemy.orm import Session

from models import TextFile


logger = logging.getLogger(__name__)
DOCUMENT_SLUGS = ("terms", "privacy", "location")
ASSET_DIR = (
    Path(__file__).resolve().parents[2]
    / "android"
    / "app"
    / "src"
    / "main"
    / "assets"
)


def seed_documents(db: Session) -> int:
    """앱에 동봉된 정책 문서를 TEXT_FILES의 최신 원본으로 동기화한다."""
    updated = 0
    for slug in DOCUMENT_SLUGS:
        source = ASSET_DIR / f"{slug}.md"
        if not source.is_file():
            logger.warning("정책 문서 원본을 찾을 수 없습니다: %s", source)
            continue
        content = source.read_text(encoding="utf-8")
        row = (
            db.query(TextFile)
            .filter(TextFile.filename == source.name)
            .order_by(TextFile.created_at.desc())
            .first()
        )
        if row is None:
            max_id = db.query(func.max(TextFile.id)).scalar() or 0
            db.add(TextFile(id=max_id + 1, filename=source.name, content=content))
            db.flush()
            updated += 1
        elif row.content != content:
            row.content = content
            updated += 1
    db.commit()
    return updated