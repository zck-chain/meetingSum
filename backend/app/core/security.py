import hashlib
import uuid
from datetime import datetime, timezone

from app.core.config import settings


def generate_id() -> str:
    return uuid.uuid4().hex


def hash_filename(original_name: str) -> str:
    ts = datetime.now(timezone.utc).isoformat().encode()
    raw = f"{original_name}-{uuid.uuid4()}".encode()
    return hashlib.sha256(raw).hexdigest()[:16]


def get_file_extension(filename: str) -> str:
    return filename.rsplit(".", 1)[-1].lower() if "." in filename else ""


def is_format_allowed(filename: str) -> bool:
    ext = get_file_extension(filename)
    return ext in settings.allowed_format_list


def safe_filename(filename: str) -> str:
    return "".join(c for c in filename if c.isalnum() or c in "._-")
