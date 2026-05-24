from app.pipeline.transcriber import transcribe, TranscriptionResult
from app.pipeline.diarizer import assign_speakers


def transcribe_audio(audio_path: str, model_name: str | None = None) -> TranscriptionResult:
    """
    Transcribe audio file and optionally run speaker diarization.
    """
    result = transcribe(audio_path, model_name)
    if result.segments:
        assign_speakers(result.segments, audio_path)
    return result
