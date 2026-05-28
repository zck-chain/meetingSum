import argparse
import json
import os
import sys
from contextlib import redirect_stdout
import whisper


def main():
    parser = argparse.ArgumentParser(description="Whisper transcription bridge for Java backend")
    parser.add_argument("--audio", required=True, help="Path to audio file")
    parser.add_argument("--model", default="medium", help="Whisper model size")
    parser.add_argument("--device", default="cpu", help="Device to use (cpu/cuda)")
    args = parser.parse_args()

    model = whisper.load_model(args.model, device=args.device)
    with open(os.devnull, "w") as devnull:
        with redirect_stdout(devnull):
            result = model.transcribe(args.audio, verbose=False)

    segments = [
        {"start": seg["start"], "end": seg["end"], "text": seg["text"].strip()}
        for seg in result["segments"]
    ]
    full_text = " ".join(s["text"] for s in segments)

    output = {
        "segments": segments,
        "full_text": full_text,
        "language": result.get("language", ""),
    }

    json.dump(output, sys.stdout, ensure_ascii=False)


if __name__ == "__main__":
    main()
