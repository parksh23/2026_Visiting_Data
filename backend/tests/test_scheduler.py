import os
import sys
import time
from pathlib import Path


os.environ["DATABASE_URL"] = "sqlite:///:memory:"
APP_DIR = Path(__file__).resolve().parents[1] / "app"
sys.path.insert(0, str(APP_DIR))

import scheduler  # noqa: E402


def test_cleanup_removes_only_expired_uploads(tmp_path, monkeypatch):
    old_image = tmp_path / "old.jpg"
    recent_image = tmp_path / "recent.jpg"
    old_image.write_bytes(b"old")
    recent_image.write_bytes(b"recent")
    expired_at = time.time() - scheduler.UPLOAD_RETENTION_SECONDS - 60
    os.utime(old_image, (expired_at, expired_at))
    monkeypatch.setattr(scheduler, "UPLOAD_DIR", tmp_path)

    scheduler.cleanup_stale_uploads_job()

    assert not old_image.exists()
    assert recent_image.exists()
