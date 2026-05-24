import os
from pathlib import Path
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "MeetingSum"
    APP_VERSION: str = "0.1.0"
    DEBUG: bool = True
    SECRET_KEY: str = "dev-secret-change-in-production"

    DATABASE_URL: str = "sqlite:///./meetingsum.db"

    REDIS_URL: str = "redis://localhost:6379"

    ASR_PROVIDER: str = "whisper_local"
    WHISPER_MODEL: str = "medium"
    WHISPER_DEVICE: str = "cpu"

    LLM_PROVIDER: str = "claude"
    ANTHROPIC_API_KEY: str = ""
    ANTHROPIC_MODEL: str = "claude-sonnet-4-6"

    MAX_FILE_SIZE_MB: int = 2048
    MAX_VIDEO_DURATION_SECONDS: int = 14400
    ALLOWED_FORMATS: str = "mp4,mov,avi,mkv,mp3,wav,m4a,webm"

    UPLOAD_DIR: str = "./uploads"
    OUTPUT_DIR: str = "./outputs"

    @property
    def max_file_size_bytes(self) -> int:
        return self.MAX_FILE_SIZE_MB * 1024 * 1024

    @property
    def allowed_format_list(self) -> list[str]:
        return [f.strip() for f in self.ALLOWED_FORMATS.split(",")]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()

Path(settings.UPLOAD_DIR).mkdir(parents=True, exist_ok=True)
Path(settings.OUTPUT_DIR).mkdir(parents=True, exist_ok=True)
