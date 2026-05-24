import logging
from pathlib import Path
from app.core.config import settings
from app.core.security import hash_filename, get_file_extension
from app.models.meeting import Meeting
from app.pipeline.audio_extractor import extract_audio, get_media_duration
from app.pipeline.transcriber import transcribe
from app.pipeline.diarizer import assign_speakers
from app.pipeline.summarizer import summarize, SummaryResult

logger = logging.getLogger(__name__)

PIPELINE_STAGES = [
    "extracting_audio",
    "transcribing",
    "summarizing",
    "exporting",
]

STAGE_WEIGHTS = {
    "extracting_audio": 20,
    "transcribing": 50,
    "summarizing": 20,
    "exporting": 10,
}


def run_pipeline(
    meeting: Meeting,
    meeting_dir: str,
    progress_callback=None,
) -> str:
    """
    Run the full AI processing pipeline on a meeting.
    Returns the path to the output directory containing generated documents.

    The progress_callback is called as: callback(stage: str, percent: int)
    """
    input_path = meeting.original_file
    if not input_path or not Path(input_path).exists():
        raise FileNotFoundError(f"Input file not found: {input_path}")

    _emit_progress(progress_callback, "extracting_audio", 0, "starting")

    # Stage 1: Audio extraction
    audio_path = extract_audio(input_path, meeting_dir)
    duration = get_media_duration(input_path)
    meeting.duration_seconds = int(duration)
    _emit_progress(progress_callback, "extracting_audio", 100, "done")

    # Stage 2: Transcription
    _emit_progress(progress_callback, "transcribing", 0, "loading model")
    transcription = transcribe(audio_path)
    assign_speakers(transcription.segments, audio_path)
    meeting.transcript_text = transcription.full_text
    _emit_progress(progress_callback, "transcribing", 100, "done")

    # Stage 3: Summarization
    _emit_progress(progress_callback, "summarizing", 0, "calling LLM")
    summary = summarize(transcription.full_text)
    meeting.summary_json = {
        "title": summary.title,
        "summary": summary.summary,
        "key_points": summary.key_points,
        "decisions": summary.decisions,
        "action_items": summary.action_items,
        "tags": summary.tags,
    }
    _emit_progress(progress_callback, "summarizing", 100, "done")

    # Stage 4: Document export
    _emit_progress(progress_callback, "exporting", 0, "generating documents")
    from app.services.export_service import export_all
    export_all(meeting, summary, transcription, meeting_dir)
    _emit_progress(progress_callback, "exporting", 100, "done")

    return meeting_dir


def _emit_progress(callback, stage: str, stage_percent: int, message: str) -> None:
    if callback is None:
        return
    try:
        callback(stage, stage_percent, message)
    except Exception:
        logger.exception("Progress callback failed")
