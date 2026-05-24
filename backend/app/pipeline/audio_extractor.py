import subprocess
from pathlib import Path
from app.core.config import settings


def extract_audio(input_path: str, output_dir: str | None = None) -> str:
    """
    Extract audio track from video/audio file using FFmpeg.
    Converts to 16kHz mono WAV for ASR compatibility.
    Returns the path to the extracted audio file.
    """
    input_file = Path(input_path)
    output_dir = Path(output_dir or settings.UPLOAD_DIR)
    output_dir.mkdir(parents=True, exist_ok=True)

    output_path = output_dir / f"{input_file.stem}_audio.wav"

    cmd = [
        "ffmpeg",
        "-i", str(input_file),
        "-ar", "16000",
        "-ac", "1",
        "-map", "0:a:0",
        "-y",
        str(output_path),
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        raise RuntimeError(f"FFmpeg audio extraction failed: {result.stderr}")

    return str(output_path)


def get_media_duration(input_path: str) -> float:
    """Get media duration in seconds using ffprobe."""
    cmd = [
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(input_path),
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        raise RuntimeError(f"ffprobe failed: {result.stderr}")

    return float(result.stdout.strip())
