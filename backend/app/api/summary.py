from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.models.meeting import Meeting

router = APIRouter(prefix="/api/v1/meetings", tags=["summary"])


@router.get("/{meeting_id}")
def get_meeting(meeting_id: str, db: Session = Depends(get_db)):
    meeting = db.query(Meeting).filter(Meeting.id == meeting_id).first()
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")

    return {
        "id": meeting.id,
        "title": meeting.title,
        "original_file": meeting.original_file,
        "original_format": meeting.original_format,
        "duration_seconds": meeting.duration_seconds,
        "file_size_bytes": meeting.file_size_bytes,
        "status": meeting.status,
        "summary_json": meeting.summary_json,
        "error_message": meeting.error_message,
        "created_at": meeting.created_at.isoformat() if meeting.created_at else None,
        "updated_at": meeting.updated_at.isoformat() if meeting.updated_at else None,
    }


@router.get("")
def list_meetings(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    status: str | None = None,
    search: str | None = None,
    db: Session = Depends(get_db),
):
    query = db.query(Meeting)

    if status:
        query = query.filter(Meeting.status == status)
    if search:
        query = query.filter(Meeting.title.ilike(f"%{search}%"))

    total = query.count()
    meetings = (
        query
        .order_by(Meeting.created_at.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return {
        "items": [
            {
                "id": m.id,
                "title": m.title,
                "original_format": m.original_format,
                "duration_seconds": m.duration_seconds,
                "file_size_bytes": m.file_size_bytes,
                "status": m.status,
                "created_at": m.created_at.isoformat() if m.created_at else None,
                "updated_at": m.updated_at.isoformat() if m.updated_at else None,
            }
            for m in meetings
        ],
        "total": total,
        "page": page,
        "page_size": page_size,
        "total_pages": (total + page_size - 1) // page_size,
    }


@router.delete("/{meeting_id}")
def delete_meeting(meeting_id: str, db: Session = Depends(get_db)):
    meeting = db.query(Meeting).filter(Meeting.id == meeting_id).first()
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")

    from pathlib import Path
    from app.core.config import settings

    if meeting.original_file:
        try:
            Path(meeting.original_file).unlink(missing_ok=True)
        except Exception:
            pass

    output_dir = Path(settings.OUTPUT_DIR) / meeting_id
    try:
        import shutil
        shutil.rmtree(output_dir, ignore_errors=True)
    except Exception:
        pass

    db.delete(meeting)
    db.commit()

    return {"detail": "deleted"}
