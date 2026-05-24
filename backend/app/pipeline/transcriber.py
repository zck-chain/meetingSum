from dataclasses import dataclass, field
from app.core.config import settings


@dataclass
class TranscriptionSegment:
    start: float
    end: float
    text: str
    speaker: str | None = None


@dataclass
class TranscriptionResult:
    segments: list[TranscriptionSegment] = field(default_factory=list)
    full_text: str = ""
    language: str = ""

    @property
    def text_with_timestamps(self) -> str:
        lines = []
        for seg in self.segments:
            ts = f"[{_fmt_time(seg.start)} -> {_fmt_time(seg.end)}]"
            prefix = f"[{seg.speaker}] " if seg.speaker else ""
            lines.append(f"{ts} {prefix}{seg.text}")
        return "\n".join(lines)


def _fmt_time(seconds: float) -> str:
    h = int(seconds // 3600)
    m = int((seconds % 3600) // 60)
    s = int(seconds % 60)
    return f"{h:02d}:{m:02d}:{s:02d}"


def transcribe(audio_path: str, model_name: str | None = None) -> TranscriptionResult:
    """
    Transcribe audio using OpenAI Whisper (local).
    Returns structured TranscriptionResult with segments and full text.
    """
    import whisper

    model_name = model_name or settings.WHISPER_MODEL
    device = settings.WHISPER_DEVICE

    model = whisper.load_model(model_name, device=device)
    result = model.transcribe(audio_path, verbose=False)

    segments = [
        TranscriptionSegment(
            start=seg["start"],
            end=seg["end"],
            text=seg["text"].strip(),
        )
        for seg in result["segments"]
    ]

    return TranscriptionResult(
        segments=segments,
        full_text=" ".join(seg.text for seg in segments),
        language=result.get("language", ""),
    )
