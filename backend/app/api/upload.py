from pathlib import Path
from fastapi import APIRouter, UploadFile, File, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.config import settings
from app.core.security import hash_filename, is_format_allowed, safe_filename
from app.models.meeting import Meeting, Task
from app.workers.process_task import process_meeting

router = APIRouter(prefix="/api/v1/meetings", tags=["upload"])


@router.post("/upload")
async def upload_meeting(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    if not file.filename:
        raise HTTPException(status_code=400, detail="Filename is required")

    if not is_format_allowed(file.filename):
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported format. Allowed: {settings.allowed_format_list}",
        )

    content = await file.read()
    file_size = len(content)

    if file_size > settings.max_file_size_bytes:
        raise HTTPException(
            status_code=413,
            detail=f"File too large. Max: {settings.MAX_FILE_SIZE_MB}MB",
        )

    safe_name = safe_filename(file.filename)
    hashed_name = f"{hash_filename(safe_name)}.{safe_name.rsplit('.', 1)[-1] if '.' in safe_name else 'bin'}"
    upload_dir = Path(settings.UPLOAD_DIR)
    upload_dir.mkdir(parents=True, exist_ok=True)
    file_path = upload_dir / hashed_name
    file_path.write_bytes(content)

    meeting = Meeting(
        title=Path(file.filename).stem,
        original_file=str(file_path),
        original_format=safe_name.rsplit(".", 1)[-1].lower() if "." in safe_name else "",
        file_size_bytes=file_size,
        status="pending",
    )
    db.add(meeting)
    db.flush()

    task = Task(
        meeting_id=meeting.id,
        status="pending",
        stage="queued",
        progress=0,
    )
    db.add(task)
    db.commit()
    db.refresh(meeting)
    db.refresh(task)

    process_meeting.delay(meeting.id, task.id)

    return {
        "meeting_id": meeting.id,
        "task_id": task.id,
        "filename": file.filename,
        "status": "pending",
    }
