from app.pipeline.summarizer import summarize, SummaryResult


def generate_summary(transcript_text: str) -> SummaryResult:
    """
    Generate a structured meeting summary from transcript text.
    """
    return summarize(transcript_text)
