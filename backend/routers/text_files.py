from fastapi import APIRouter, Depends, UploadFile, File, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.models import TextFile

router = APIRouter(
    prefix="/text-files",
    tags=["text-files"]
)


@router.post("/upload")
async def upload_text_file(
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    if not file.filename.endswith(".txt"):
        raise HTTPException(status_code=400, detail="txt 파일만 업로드할 수 있습니다.")

    raw = await file.read()

    try:
        content = raw.decode("utf-8")
    except UnicodeDecodeError:
        content = raw.decode("cp949")

    text_file = TextFile(
        filename=file.filename,
        content=content
    )

    db.add(text_file)
    db.commit()
    db.refresh(text_file)

    return {
        "message": "txt file uploaded",
        "id": text_file.id,
        "filename": text_file.filename,
        "content_preview": text_file.content[:100]
    }


@router.get("")
def get_text_files(db: Session = Depends(get_db)):
    files = db.query(TextFile).all()

    return [
        {
            "id": file.id,
            "filename": file.filename,
            "content_preview": file.content[:100],
            "created_at": file.created_at
        }
        for file in files
    ]


@router.get("/{file_id}")
def get_text_file(file_id: int, db: Session = Depends(get_db)):
    text_file = db.query(TextFile).filter(TextFile.id == file_id).first()

    if text_file is None:
        raise HTTPException(status_code=404, detail="파일을 찾을 수 없습니다.")

    return {
        "id": text_file.id,
        "filename": text_file.filename,
        "content": text_file.content,
        "created_at": text_file.created_at
    }