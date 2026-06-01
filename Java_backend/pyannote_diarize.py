#!/usr/bin/env python3
"""
pyannote-audio 说话人分离桥接脚本
用法: python pyannote_diarize.py --audio <audio.wav> --hf-token <token> --device cpu
输出到 stdout: {"segments": [{"start": 0.0, "end": 5.2, "speaker": "SPEAKER_00"}, ...]}

环境要求: pip install pyannote.audio torch
"""
import argparse
import json
import os
import sys


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--audio", required=True, help="16kHz mono WAV file")
    parser.add_argument("--hf-token", default="", help="HuggingFace access token (for gated model)")
    parser.add_argument("--device", default="cpu", choices=["cpu", "cuda"])
    args = parser.parse_args()

    if not os.path.exists(args.audio):
        print(json.dumps({"error": f"Audio file not found: {args.audio}"}), file=sys.stderr)
        sys.exit(1)

    try:
        from pyannote.audio import Pipeline
        import torch
    except ImportError as e:
        print(json.dumps({"error": f"pyannote.audio not installed: {e}"}), file=sys.stderr)
        sys.exit(1)

    try:
        hf_token = args.hf_token if args.hf_token else None
        pipeline = Pipeline.from_pretrained(
            "pyannote/speaker-diarization-3.1",
            use_auth_token=hf_token,
        )
        pipeline.to(torch.device(args.device))

        diarization = pipeline(args.audio)

        segments = []
        for turn, _, speaker in diarization.itertracks(yield_label=True):
            segments.append({
                "start": round(turn.start, 2),
                "end": round(turn.end, 2),
                "speaker": speaker,
            })

        json.dump({"segments": segments}, sys.stdout, ensure_ascii=False)

    except Exception as e:
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
