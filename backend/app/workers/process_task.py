import logging
from pathlib import Path
from datetime import datetime, timezone
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import SessionLocal
from app.models.meeting import Meeting, Task
from app.pipeline.orchestrator import run_pipeline
from app.workers.celery_app import celery_app

logger = logging.getLogger(__name__)


def _update_progress(task_id: str, stage: str, percent: int, message: str) -> None:
    """Update task stage and progress in the database."""
    db = SessionLocal()
    try:
        task = db.query(Task).filter(Task.id == task_id).first()
        if task:
            task.stage = stage
            task.progress = min(percent, 100)
            db.commit()
    finally:
        db.close()


@celery_app.task(bind=True, max_retries=2)
def process_meeting(self, meeting_id: str, task_id: str) -> dict:
    """
    Main Celery task: runs the full AI pipeline on a meeting.
    Updates task progress throughout and handles failures.
    """
    db = SessionLocal()
    try:
        meeting = db.query(Meeting).filter(Meeting.id == meeting_id).first()
        task = db.query(Task).filter(Task.id == task_id).first()

        if not meeting or not task:
            raise ValueError(f"Meeting {meeting_id} or Task {task_id} not found")

        meeting.status = "processing"
        task.status = "processing"
        db.commit()

        meeting_dir = str(Path(settings.OUTPUT_DIR) / meeting_id)
        Path(meeting_dir).mkdir(parents=True, exist_ok=True)

        def progress_cb(stage: str, percent: int, message: str):
            _update_progress(task_id, stage, percent, message)

        run_pipeline(meeting, meeting_dir, progress_callback=progress_cb)

        task.stage = "completed"
        task.progress = 100
        task.status = "completed"
        task.completed_at = datetime.now(timezone.utc)
        meeting.status = "completed"
        meeting.updated_at = datetime.now(timezone.utc)
        db.commit()

        return {"meeting_id": meeting_id, "status": "completed"}

    except Exception as exc:
        logger.exception(f"Pipeline failed for meeting {meeting_id}")
        try:
            meeting = db.query(Meeting).filter(Meeting.id == meeting_id).first()
            task = db.query(Task).filter(Task.id == task_id).first()
            if task:
                task.status = "failed"
                task.stage = "failed"
            if meeting:
                meeting.status = "failed"
                meeting.error_message = str(exc)
                meeting.updated_at = datetime.now(timezone.utc)
            db.commit()
        except Exception:
            logger.exception("Failed to update error state")

        raise self.retry(exc=exc, countdown=60)

    finally:
        db.close()
