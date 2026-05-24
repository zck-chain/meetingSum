# MeetingSum — 会议学习视频智能总结软件 开发文档

## 1. 项目概述

### 1.1 项目背景

在日常工作学习过程中，会议记录和学习视频的知识整理需要耗费大量时间。用户需要一款工具，能够将会议或学习视频自动转化为结构化的 Markdown 或 Word 文档，提炼核心要点，节省手动整理的时间成本。

### 1.2 项目目标

构建一款名为 **MeetingSum** 的桌面/Web 应用，用户上传会议录制或学习视频后，系统自动完成：

- 语音转文字（ASR）
- 智能摘要生成
- 关键要点、行动项、决策记录提取
- 输出结构化的 `.md` 或 `.docx` 文件

### 1.3 目标用户

- 需要整理会议记录的知识工作者
- 需要从学习视频中提取笔记的学生/自学者
- 需要归档会议纪要的项目管理人员

---

## 2. 功能需求

### 2.1 核心功能

| 模块 | 功能 | 优先级 |
|------|------|--------|
| 视频导入 | 支持本地上传视频/音频文件，支持 URL 链接导入 | P0 |
| 语音识别 | 将视频/音频中的语音转为文字（支持中英文） | P0 |
| 说话人分离 | 识别不同说话人，标注对话段落 | P1 |
| 智能摘要 | 基于转录文本生成会议摘要 | P0 |
| 要点提取 | 自动提取关键讨论点、决策、行动项 | P0 |
| 文档导出 | 导出为 Markdown (.md) 文件 | P0 |
| 文档导出 | 导出为 Word (.docx) 文件 | P1 |
| 时间戳标注 | 在摘要中标注关键内容对应的时间点 | P2 |

### 2.2 输出文档结构

生成的 Markdown / Word 文档应包含以下结构化内容：

```markdown
# [会议/视频标题]

## 基本信息
- 日期：2026-05-24
- 时长：01:23:45
- 参会人/主讲人：张三、李四

## 会议摘要
[一段 200-300 字的整体摘要]

## 关键讨论点
1. **议题一**：[内容摘要]
2. **议题二**：[内容摘要]

## 决策记录
- [ ] 决策一：...
- [ ] 决策二：...

## 行动项
- [ ] 行动项一 — 负责人：张三 — 截止日期：2026-05-30
- [ ] 行动项二 — 负责人：李四 — 截止日期：2026-06-01

## 完整转录文本（可选）
[带时间戳的完整转录]
```

### 2.3 辅助功能

- 历史记录管理：查看、搜索、删除历史会议记录
- 摘要模板自定义：用户可自定义输出模板
- 批量处理：支持一次上传多个文件排队处理
- 导出格式选择：Markdown / Word / PDF / 纯文本

---

## 3. 技术架构

### 3.1 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    前端 (Frontend)                     │
│              React + TypeScript + Tailwind            │
│          Electron (桌面端) / Web (浏览器端)            │
└──────────────────────┬───────────────────────────────┘
                       │ HTTP / WebSocket
┌──────────────────────┴───────────────────────────────┐
│                  后端服务 (Backend)                     │
│                  Python FastAPI                        │
│                                                        │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │ 文件管理  │  │ 任务队列  │  │ 用户/会话管理     │  │
│  │  Module  │  │  Celery  │  │     Module        │  │
│  └──────────┘  └──────────┘  └───────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │                AI 处理管道 (Pipeline)              │ │
│  │                                                    │ │
│  │  音频提取 → 语音转文字 → 文本后处理 → 摘要生成     │ │
│  │                                                    │ │
│  │   FFmpeg     Whisper     规则/NER    LLM API      │ │
│  └──────────────────────────────────────────────────┘ │
│                                                        │
│  ┌──────────────────────────────────────────────────┐ │
│  │              文档生成引擎 (DocEngine)               │ │
│  │                                                    │ │
│  │  模板渲染 → Markdown 生成 → Word 生成 → PDF 生成   │ │
│  └──────────────────────────────────────────────────┘ │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────┴───────────────────────────────┐
│                   存储 & 中间件                         │
│   PostgreSQL / SQLite  │  Redis  │  MinIO / 本地文件   │
└──────────────────────────────────────────────────────┘
```

### 3.2 技术选型

| 层级 | 技术 | 选型理由 |
|------|------|----------|
| 前端框架 | React 18 + TypeScript | 生态丰富，组件化开发效率高 |
| 桌面端壳 | Electron | 跨平台桌面应用，可访问本地文件系统 |
| UI 组件库 | Ant Design / shadcn/ui | 成熟的 React 组件库 |
| 状态管理 | Zustand | 轻量级，TS 友好 |
| 后端框架 | Python FastAPI | 异步支持好，AI/ML 生态原生支持 |
| 异步任务 | Celery + Redis | 视频处理是长任务，需要异步队列 |
| 语音识别 | OpenAI Whisper (本地) 或 阿里云/讯飞 ASR | Whisper 离线可用，云服务识别率更高 |
| LLM 摘要 | Claude API / GPT-4 API | 摘要质量高，支持长上下文 |
| 音频处理 | FFmpeg | 提取音频、格式转换 |
| 文档生成 | python-docx / markdown 库 | 成熟稳定 |
| 数据库 | SQLite (单机) / PostgreSQL (多用户) | 初期 SQLite 零部署成本 |
| 缓存/队列 | Redis | Celery 默认 broker |
| 对象存储 | 本地文件系统 / MinIO | 初期本地存储即可 |

### 3.3 处理管道详细设计

```
用户上传视频
    │
    ▼
┌─────────────────┐
│ 1. 文件校验      │  格式检查 (mp4/mov/avi/mkv/mp3/wav/m4a)
│    & 预处理      │  文件大小限制、病毒扫描
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. 音频提取      │  FFmpeg: 提取音轨 → 16kHz mono WAV
│                 │  命令: ffmpeg -i input.mp4 -ar 16000 -ac 1 output.wav
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. 语音分段      │  VAD (Voice Activity Detection)
│                 │  Silero VAD / WebRTC VAD
│                 │  将长音频切分为 30-60s 片段
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. 语音转文字    │  Whisper large-v3 / 云 ASR 服务
│                 │  输出: 带时间戳的分段文本
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. 说话人分离    │  pyannote-audio (Speaker Diarization)
│  (可选)         │  标注每段话的说话人
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 6. 文本后处理    │  标点恢复、段落合并、去口语化
│                 │  敏感信息脱敏 (手机号/邮箱等)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 7. 摘要生成      │  LLM API (Claude / GPT-4)
│                 │  Prompt 工程引导结构化输出
│                 │  输出: JSON 结构化数据
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 8. 文档渲染      │  模板引擎填充 → Markdown / DOCX / PDF
│                 │  文件写入磁盘
└────────┬────────┘
         │
         ▼
    返回下载链接给前端
```

---

## 4. 项目结构

```
meeting-summarizer/
├── frontend/                      # 前端项目
│   ├── src/
│   │   ├── components/            # UI 组件
│   │   │   ├── Uploader/          # 文件上传组件
│   │   │   ├── HistoryList/       # 历史记录列表
│   │   │   ├── SummaryViewer/     # 摘要预览组件
│   │   │   ├── ExportPanel/       # 导出设置面板
│   │   │   └── Layout/            # 布局组件
│   │   ├── pages/
│   │   │   ├── Home.tsx           # 首页（上传页）
│   │   │   ├── History.tsx        # 历史记录页
│   │   │   ├── Summary.tsx        # 摘要详情页
│   │   │   └── Settings.tsx       # 设置页
│   │   ├── stores/                # Zustand 状态管理
│   │   ├── api/                   # API 请求封装
│   │   ├── types/                 # TypeScript 类型定义
│   │   └── utils/                 # 工具函数
│   ├── package.json
│   └── electron/                  # Electron 主进程
│       └── main.ts
│
├── backend/                       # 后端项目
│   ├── app/
│   │   ├── api/                   # API 路由
│   │   │   ├── upload.py          # 上传接口
│   │   │   ├── tasks.py           # 任务状态查询
│   │   │   ├── summary.py         # 摘要 CRUD
│   │   │   └── export.py          # 导出接口
│   │   ├── core/
│   │   │   ├── config.py          # 配置管理
│   │   │   ├── security.py        # 安全相关
│   │   │   └── database.py        # 数据库连接
│   │   ├── models/                # SQLAlchemy 数据模型
│   │   │   ├── meeting.py
│   │   │   └── task.py
│   │   ├── services/              # 业务逻辑层
│   │   │   ├── audio_service.py   # 音频处理
│   │   │   ├── asr_service.py     # 语音识别
│   │   │   ├── summary_service.py # 摘要生成
│   │   │   └── export_service.py  # 文档导出
│   │   ├── pipeline/              # AI 处理管道
│   │   │   ├── orchestrator.py    # 管道编排器
│   │   │   ├── audio_extractor.py # 音频提取
│   │   │   ├── transcriber.py     # 转录器
│   │   │   ├── diarizer.py        # 说话人分离
│   │   │   └── summarizer.py      # 摘要器
│   │   ├── templates/             # 文档模板
│   │   │   ├── default_md.jinja2
│   │   │   └── default_docx.jinja2
│   │   └── workers/               # Celery 任务
│   │       └── process_task.py    # 主处理任务
│   ├── requirements.txt
│   └── alembic/                   # 数据库迁移
│
├── docker-compose.yml             # 容器编排
├── .env.example                   # 环境变量示例
└── README.md
```

---

## 5. 核心接口设计

### 5.1 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/meetings/upload` | 上传视频文件，创建处理任务 |
| GET | `/api/v1/meetings/{id}` | 获取会议处理详情 |
| GET | `/api/v1/meetings` | 获取历史会议列表（分页） |
| DELETE | `/api/v1/meetings/{id}` | 删除会议记录 |
| GET | `/api/v1/tasks/{id}/status` | 查询处理任务状态 |
| GET | `/api/v1/meetings/{id}/export?format=md` | 下载导出的 Markdown 文件 |
| GET | `/api/v1/meetings/{id}/export?format=docx` | 下载导出的 Word 文件 |

### 5.2 处理任务状态机

```
pending → processing → completed
                      → failed
                      → cancelled
```

### 5.3 WebSocket 事件（可选）

用于实时推送处理进度：

```
ws://localhost:8000/ws/tasks/{task_id}

事件类型：
- progress: { stage: "extracting_audio", percent: 10 }
- progress: { stage: "transcribing", percent: 40 }
- progress: { stage: "summarizing", percent: 80 }
- complete: { meeting_id: "xxx" }
- error: { message: "..." }
```

---

## 6. LLM Prompt 设计（核心）

### 6.1 系统 Prompt

```
你是一个专业的会议纪要整理助手。你的任务是根据提供的会议转录文本，
生成结构化的会议纪要。请严格按照以下 JSON 格式输出（不要输出其他内容）：

{
  "title": "会议标题（从内容中推断，不超过30字）",
  "summary": "会议整体摘要，200-300字，概括会议目的、主要讨论内容和结论",
  "key_points": [
    {"topic": "议题名称", "content": "讨论内容摘要", "importance": "high|medium|low"}
  ],
  "decisions": [
    {"content": "决策内容", "proposer": "提出人"}
  ],
  "action_items": [
    {"content": "行动项", "assignee": "负责人", "deadline": "截止日期或null"}
  ],
  "tags": ["标签1", "标签2"]
}

要求：
- 使用中文输出
- 关键讨论点按重要性排序
- 行动项必须包含负责人和截止日期（如果原文提到）
- 去除口语化的填充词和重复内容
- 保持客观，不添加原文未提及的信息
```

### 6.2 分块处理策略

对于长视频（> 1 小时），采用分块摘要策略：

1. 按主题/时间段将转录文本分为多个 chunk（每个 chunk 约 4000-8000 字）
2. 对每个 chunk 独立生成段落摘要
3. 将所有段落摘要拼接后，进行一次全局摘要生成
4. 最终输出结构化的完整会议纪要

```
长文本 → 分段 → [Chunk1摘要] [Chunk2摘要] ... [ChunkN摘要]
                      │
                      ▼
              拼接 → 全局摘要 → 最终结构化输出
```

---

## 7. 数据库设计

### 7.1 ER 图（核心表）

```sql
-- 会议记录表
CREATE TABLE meetings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(255),
    original_file   VARCHAR(500),       -- 原始文件路径
    original_format VARCHAR(20),        -- mp4/mp3/wav/...
    duration_seconds INTEGER,
    file_size_bytes  BIGINT,
    status          VARCHAR(20) DEFAULT 'pending',  -- pending/processing/completed/failed
    transcript_text TEXT,               -- 完整转录文本
    summary_json    JSONB,              -- 结构化摘要数据
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- 处理任务表
CREATE TABLE tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id      UUID REFERENCES meetings(id) ON DELETE CASCADE,
    stage           VARCHAR(30),        -- extracting_audio/transcribing/summarizing/exporting
    progress        INTEGER DEFAULT 0,  -- 0-100
    status          VARCHAR(20) DEFAULT 'pending',
    result_path     VARCHAR(500),       -- 输出文件路径
    created_at      TIMESTAMP DEFAULT NOW(),
    completed_at    TIMESTAMP
);
```

---

## 8. 开发计划

### 8.1 阶段划分

#### Phase 1：MVP 最小可用版本（2-3 周）

- [ ] 项目脚手架搭建（前后端初始化）
- [ ] 视频上传功能（本地文件）
- [ ] FFmpeg 音频提取
- [ ] Whisper 语音转文字（本地小模型 base/small）
- [ ] LLM 摘要生成（调用 Claude API）
- [ ] Markdown 文件导出
- [ ] 基础 Web UI（上传 → 等待 → 查看/下载）

#### Phase 2：功能增强（2-3 周）

- [ ] 说话人分离（Speaker Diarization）
- [ ] Word (.docx) 导出
- [ ] 处理进度实时推送（WebSocket）
- [ ] 摘要模板自定义
- [ ] 历史记录管理（搜索/删除/分页）
- [ ] 错误处理与重试机制

#### Phase 3：体验优化（1-2 周）

- [ ] Electron 桌面端打包
- [ ] 拖拽上传 + 批量处理
- [ ] 时间戳回链（点击摘要跳转到视频对应时刻）
- [ ] 多语言支持（UI 国际化）
- [ ] 离线模式（全部本地处理，不依赖云 API）

#### Phase 4：生产就绪（1-2 周）

- [ ] 用户认证系统
- [ ] 数据加密存储
- [ ] 性能优化（大文件分片上传、并行处理）
- [ ] Docker 一键部署
- [ ] 使用文档与帮助中心

### 8.2 技术风险与应对

| 风险 | 影响 | 应对方案 |
|------|------|----------|
| Whisper 本地运行慢 | 用户体验差 | Phase 1 提供云 ASR 选项（阿里云/讯飞 API）作为备选 |
| LLM API 成本高 | 运营成本 | 长视频采用分块 + 递归摘要策略，控制 token 消耗 |
| 中文识别准确率不足 | 摘要质量差 | 使用 Whisper large-v3 模型 + 中文微调版本（belle-whisper） |
| 大文件上传超时 | 上传失败 | 分片上传 + 断点续传 |

---

## 9. 环境与部署

### 9.1 开发环境

```bash
# 后端
Python >= 3.11
FFmpeg >= 5.0
Redis >= 7.0

# 前端
Node.js >= 20
pnpm >= 8

# AI 依赖 (可选 GPU)
CUDA 12.x (用于 Whisper 本地推理加速)
```

### 9.2 Docker 部署

```yaml
# docker-compose.yml
version: "3.9"
services:
  backend:
    build: ./backend
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/meetingsum
      - REDIS_URL=redis://redis:6379
      - LLM_API_KEY=${LLM_API_KEY}
      - ASR_PROVIDER=whisper_local  # 或 aliyun / xunfei
    volumes:
      - ./uploads:/app/uploads
      - ./outputs:/app/outputs
    depends_on:
      - db
      - redis

  worker:
    build: ./backend
    command: celery -A app.workers worker --loglevel=info
    environment:
      - DATABASE_URL=postgresql://user:pass@db:5432/meetingsum
      - REDIS_URL=redis://redis:6379
      - LLM_API_KEY=${LLM_API_KEY}
    volumes:
      - ./uploads:/app/uploads
      - ./outputs:/app/outputs
    depends_on:
      - db
      - redis

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: meetingsum
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine

volumes:
  pgdata:
```

---

## 10. 配置项说明

```ini
# .env 文件

# 应用配置
APP_NAME=MeetingSum
APP_VERSION=0.1.0
DEBUG=true
SECRET_KEY=your-secret-key-here

# 数据库
DATABASE_URL=sqlite:///./meetingsum.db       # 开发环境用 SQLite
# DATABASE_URL=postgresql://user:pass@localhost:5432/meetingsum  # 生产环境

# Redis
REDIS_URL=redis://localhost:6379

# 语音识别
ASR_PROVIDER=whisper_local                    # whisper_local | aliyun | azure
WHISPER_MODEL=medium                          # tiny/base/small/medium/large-v3
WHISPER_DEVICE=cpu                            # cpu | cuda

# LLM 配置
LLM_PROVIDER=claude                           # claude | openai
ANTHROPIC_API_KEY=sk-ant-xxx
ANTHROPIC_MODEL=claude-sonnet-4-6
# OPENAI_API_KEY=sk-xxx
# OPENAI_MODEL=gpt-4o

# 文件限制
MAX_FILE_SIZE_MB=2048                         # 最大上传文件 2GB
MAX_VIDEO_DURATION_SECONDS=14400              # 最长视频 4 小时
ALLOWED_FORMATS=mp4,mov,avi,mkv,mp3,wav,m4a,webm

# 存储路径
UPLOAD_DIR=./uploads
OUTPUT_DIR=./outputs
```

---

## 11. 非功能性需求

| 指标 | 目标值 |
|------|--------|
| 音频提取成功率 | >= 99% |
| 语音识别准确率（中文普通话） | >= 90% (Whisper large-v3) |
| 1 小时视频端到端处理时间 | <= 15 分钟（GPU）/ <= 40 分钟（CPU） |
| 摘要信息完整度 | 覆盖原文 80% 以上的关键信息点 |
| 单文件最大支持 | 2GB / 4 小时 |
| 并发处理能力 | 单机 2-3 个任务并行 |

---

## 12. 附录

### 12.1 关键依赖包

**Python (requirements.txt):**
```
fastapi==0.115.*
uvicorn[standard]==0.32.*
celery==5.4.*
redis==5.2.*
sqlalchemy==2.0.*
alembic==1.14.*
python-multipart==0.0.*
openai-whisper==20240930
ffmpeg-python==0.2.*
anthropic==0.40.*
python-docx==1.1.*
jinja2==3.1.*
pydantic==2.10.*
pydantic-settings==2.7.*
```

**Node.js (package.json):**
```json
{
  "dependencies": {
    "react": "^19.0.0",
    "react-router-dom": "^7.0.0",
    "zustand": "^5.0.0",
    "antd": "^5.22.0",
    "axios": "^1.7.0",
    "@ant-design/icons": "^5.5.0",
    "react-dropzone": "^14.3.0"
  },
  "devDependencies": {
    "typescript": "^5.7.0",
    "vite": "^6.0.0",
    "tailwindcss": "^4.0.0",
    "electron": "^33.0.0"
  }
}
```

### 12.2 参考资料

- OpenAI Whisper: https://github.com/openai/whisper
- pyannote-audio (说话人分离): https://github.com/pyannote/pyannote-audio
- Anthropic API 文档: https://docs.anthropic.com
- FFmpeg 文档: https://ffmpeg.org/documentation.html
