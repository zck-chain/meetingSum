from pathlib import Path
from app.pipeline.audio_extractor import extract_audio, get_media_duration


def process_audio(file_path: str, output_dir: str) -> dict:
    """
    Extract audio from media file and return metadata.
    """
    audio_path = extract_audio(file_path, output_dir)
    duration = get_media_duration(file_path)
    audio_size = Path(audio_path).stat().st_size

    return {
        "audio_path": audio_path,
        "duration_seconds": duration,
        "audio_size_bytes": audio_size,
    }
