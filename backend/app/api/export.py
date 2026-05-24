from pathlib import Path
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.config import settings
from app.models.meeting import Meeting

router = APIRouter(prefix="/api/v1/meetings", tags=["export"])


@router.get("/{meeting_id}/export")
def export_meeting(
    meeting_id: str,
    format: str = Query("md", description="Export format: md, docx, transcript"),
    db: Session = Depends(get_db),
):
    meeting = db.query(Meeting).filter(Meeting.id == meeting_id).first()
    if not meeting:
        raise HTTPException(status_code=404, detail="Meeting not found")

    if meeting.status != "completed":
        raise HTTPException(status_code=400, detail="Meeting processing not yet completed")

    output_dir = Path(settings.OUTPUT_DIR) / meeting_id

    file_map = {
        "md": output_dir / f"{meeting_id}_summary.md",
        "transcript": output_dir / f"{meeting_id}_transcript.md",
        "docx": output_dir / f"{meeting_id}_summary.docx",
    }

    if format not in file_map:
        raise HTTPException(status_code=400, detail=f"Unsupported format: {format}")

    file_path = file_map[format]
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Export file not found: {format}")

    media_type_map = {
        "md": "text/markdown",
        "transcript": "text/markdown",
        "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    }

    download_name = {
        "md": f"{meeting.title or 'summary'}_摘要.md",
        "transcript": f"{meeting.title or 'transcript'}_转录.md",
        "docx": f"{meeting.title or 'summary'}_摘要.docx",
    }

    return FileResponse(
        str(file_path),
        media_type=media_type_map.get(format, "application/octet-stream"),
        filename=download_name.get(format, file_path.name),
    )
