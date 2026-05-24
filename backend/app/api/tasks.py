from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.models.meeting import Task

router = APIRouter(prefix="/api/v1/tasks", tags=["tasks"])


@router.get("/{task_id}/status")
def get_task_status(task_id: str, db: Session = Depends(get_db)):
    task = db.query(Task).filter(Task.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")

    return {
        "task_id": task.id,
        "meeting_id": task.meeting_id,
        "stage": task.stage,
        "progress": task.progress,
        "status": task.status,
        "result_path": task.result_path,
        "created_at": task.created_at.isoformat() if task.created_at else None,
        "completed_at": task.completed_at.isoformat() if task.completed_at else None,
    }
