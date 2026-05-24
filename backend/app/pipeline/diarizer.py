from app.pipeline.transcriber import TranscriptionSegment


def assign_speakers(
    segments: list[TranscriptionSegment],
    audio_path: str | None = None,
) -> list[TranscriptionSegment]:
    """
    Stub for speaker diarization. In production, integrate pyannote-audio
    to label each segment with a speaker ID.

    Currently assigns placeholder labels based on turn-taking heuristics.
    """
    if not segments:
        return segments

    speaker = "Speaker_A"
    for i, seg in enumerate(segments):
        gap = seg.start - (segments[i - 1].end if i > 0 else 0)
        if gap > 2.0:
            speaker = "Speaker_B" if speaker == "Speaker_A" else "Speaker_A"
        seg.speaker = speaker

    return segments
