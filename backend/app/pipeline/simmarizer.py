import json
from dataclasses import dataclass
from app.core.config import settings

SYSTEM_PROMPT = """你是一个专业的会议纪要整理助手。你的任务是根据提供的会议转录文本，生成结构化的会议纪要。请严格按照以下 JSON 格式输出（不要输出其他内容）：

{
  "title": "会议标题（从内容中推断，不超过30字）",
  "summary": "会议整体摘要，200-300字，概括会议目的、主要讨论内容和结论",
  "key_points": [
    {"topic": "议题名称", "content": "讨论内容摘要", "importance": "high|medium|low"}
  ],
  "decisions": [
    {"content": "决策内容", "proposer": "提出人或null"}
  ],
  "action_items": [
    {"content": "行动项", "assignee": "负责人或null", "deadline": "截止日期或null"}
  ],
  "tags": ["标签1", "标签2"]
}

要求：
- 使用中文输出
- 关键讨论点按重要性排序
- 行动项必须包含负责人和截止日期（如果原文提到）
- 去除口语化的填充词和重复内容
- 保持客观，不添加原文未提及的信息"""

CHUNK_SIZE = 4000


@dataclass
class SummaryResult:
    title: str
    summary: str
    key_points: list[dict]
    decisions: list[dict]
    action_items: list[dict]
    tags: list[str]


def _call_llm(prompt: str, max_tokens: int = 4096) -> str:
    """Call the configured LLM provider and return the response text."""
    if settings.LLM_PROVIDER == "claude":
        import anthropic
        client = anthropic.Anthropic(api_key=settings.ANTHROPIC_API_KEY)
        message = client.messages.create(
            model=settings.ANTHROPIC_MODEL,
            max_tokens=max_tokens,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": prompt}],
        )
        return message.content[0].text
    else:
        raise ValueError(f"Unsupported LLM provider: {settings.LLM_PROVIDER}")


def _parse_summary_json(raw: str) -> SummaryResult:
    """Parse LLM output into SummaryResult. Handles common JSON wrapping issues."""
    text = raw.strip()
    if text.startswith("```"):
        text = text.split("\n", 1)[1]
        if text.endswith("```"):
            text = text[:-3]
        text = text.strip()
    if text.startswith("```json"):
        text = text[7:].strip()
        if text.endswith("```"):
            text = text[:-3]
        text = text.strip()

    data = json.loads(text)
    return SummaryResult(
        title=data.get("title", ""),
        summary=data.get("summary", ""),
        key_points=data.get("key_points", []),
        decisions=data.get("decisions", []),
        action_items=data.get("action_items", []),
        tags=data.get("tags", []),
    )


def _chunk_text(text: str, chunk_size: int = CHUNK_SIZE) -> list[str]:
    """Split text into roughly equal chunks without breaking words."""
    if len(text) <= chunk_size:
        return [text]
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        if end < len(text):
            brk = text.rfind("\n", start, end)
            if brk == -1 or brk < start + chunk_size // 2:
                brk = text.rfind("。", start, end)
            if brk == -1 or brk < start + chunk_size // 2:
                brk = text.rfind(" ", start, end)
            if brk != -1 and brk > start:
                end = brk + 1
        chunks.append(text[start:end])
        start = end
    return chunks


def summarize(transcript_text: str) -> SummaryResult:
    """
    Generate a structured meeting summary from the transcript.
    For long transcripts, uses chunked summarization: summarize each chunk,
    then produce a global summary from the chunk summaries.
    """
    if not transcript_text.strip():
        raise ValueError("Transcript text is empty")

    chunks = _chunk_text(transcript_text)

    if len(chunks) == 1:
        prompt = f"请根据以下会议转录文本生成结构化的会议纪要：\n\n{transcript_text}"
        raw = _call_llm(prompt)
        return _parse_summary_json(raw)

    chunk_summaries = []
    for i, chunk in enumerate(chunks):
        prompt = f"请对以下会议转录文本片段（第{i+1}/{len(chunks)}部分）生成段落摘要：\n\n{chunk}"
        raw = _call_llm(prompt, max_tokens=1024)
        chunk_summaries.append(raw.strip())

    combined = "\n".join(
        f"第{i+1}部分摘要：\n{s}" for i, s in enumerate(chunk_summaries)
    )
    global_prompt = (
        f"以下是多个会议片段的摘要，请基于这些摘要生成最终的完整结构化会议纪要：\n\n{combined}"
    )
    raw = _call_llm(global_prompt)
    return _parse_summary_json(raw)
