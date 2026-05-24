# MeetingSum — 会议学习视频智能总结工具

上传会议录屏或学习视频，自动生成结构化的会议纪要（Markdown / Word 文档），支持语音转文字、说话人分离、AI 智能摘要。

## 功能

- **多格式上传** — 支持 MP4 / MOV / AVI / MKV / MP3 / WAV / M4A / WebM 等音视频格式，最大 2GB / 4 小时
- **语音转写** — 基于 OpenAI Whisper 本地离线转写，支持中英文
- **说话人分离** — 自动识别并标注不同说话人的对话段落
- **AI 摘要** — 调用 Claude API / GPT-4 生成结构化摘要，提取关键讨论点、决策记录、行动项
- **文档导出** — 一键导出为 Markdown 或 Word 文件
- **历史管理** — 搜索、查看、删除历史会议记录
- **实时进度** — 处理进度实时推送（音频提取 → 转写 → 摘要生成）

## 输出文档示例

```markdown
# [会议标题]

## 基本信息
- 日期：2026-05-24
- 时长：01:23:45
- 主讲人：张三、李四

## 会议摘要
[200-300 字整体摘要]

## 关键讨论点
1. 议题一：...
2. 议题二：...

## 决策记录
- [ ] 决策一
- [ ] 决策二

## 行动项
- [ ] 行动项一 — 负责人：张三 — 截止日期：2026-05-30
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | React 19 + TypeScript + Vite + Ant Design + Zustand + Tailwind CSS |
| 后端 | Python FastAPI + Celery + SQLAlchemy + Alembic |
| AI | OpenAI Whisper（语音识别）+ Claude API（摘要生成） |
| 中件间 | Redis（任务队列） |
| 音视频 | FFmpeg（音频提取） |
| 数据库 | SQLite（开发）/ PostgreSQL（生产） |

## 项目结构

```
meeting-summarizer/
├── frontend/                # React 前端
│   ├── src/
│   │   ├── components/      # UI 组件
│   │   │   ├── Uploader/    # 文件上传
│   │   │   ├── HistoryList/ # 历史记录
│   │   │   ├── SummaryViewer/ # 摘要预览
│   │   │   ├── ExportPanel/ # 导出面板
│   │   │   └── Layout/      # 布局
│   │   ├── pages/           # 页面（Home / History / Summary / Settings）
│   │   ├── stores/          # Zustand 状态管理
│   │   ├── api/             # API 请求封装
│   │   ├── types/           # TS 类型
│   │   └── utils/           # 工具函数
│   └── package.json
├── backend/                 # FastAPI 后端
│   ├── app/
│   │   ├── api/             # REST API 路由
│   │   ├── core/            # 配置 / 数据库 / 安全
│   │   ├── models/          # SQLAlchemy 数据模型
│   │   ├── services/        # 业务逻辑层
│   │   ├── pipeline/        # AI 处理管道（音频提取→转写→分离→摘要）
│   │   ├── templates/       # Jinja2 文档模板
│   │   └── workers/         # Celery 后台任务
│   └── requirements.txt
├── docker-compose.yml       # 一键部署
└── .env.example             # 环境变量模板
```

## 快速开始

### 前置依赖

- Python >= 3.11
- Node.js >= 20
- FFmpeg >= 5.0
- Redis >= 7.0

### Docker 一键部署（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/zck-chain/meetingSum.git
cd meetingSum

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入你的 ANTHROPIC_API_KEY

# 3. 启动
docker compose up -d

# 4. 访问
# 前端: http://localhost:3000
# 后端 API: http://localhost:8000
# API 文档: http://localhost:8000/docs
```

### 手动开发环境

**后端：**

```bash
cd backend

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 复制环境配置
cp .env.example .env
# 编辑 .env 填入 API Key

# 启动 Redis（如果没有运行中的 Redis）
docker run -d -p 6379:6379 redis:7-alpine

# 启动后端
uvicorn app.main:app --reload --port 8000

# 另开终端启动 Celery Worker
celery -A app.workers.celery_app worker --loglevel=info
```

**前端：**

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/meetings/upload` | 上传音视频文件 |
| `GET` | `/api/v1/meetings` | 获取历史会议列表 |
| `GET` | `/api/v1/meetings/{id}` | 获取会议详情 |
| `DELETE` | `/api/v1/meetings/{id}` | 删除会议记录 |
| `GET` | `/api/v1/tasks/{id}/status` | 查询任务处理进度 |
| `GET` | `/api/v1/meetings/{id}/export?format=md` | 导出 Markdown |
| `GET` | `/api/v1/meetings/{id}/export?format=docx` | 导出 Word |

## 配置项

在 `.env` 中配置：

```ini
# 语音识别
ASR_PROVIDER=whisper_local     # whisper_local | aliyun | azure
WHISPER_MODEL=medium           # tiny / base / small / medium / large-v3
WHISPER_DEVICE=cpu             # cpu | cuda

# LLM 摘要
LLM_PROVIDER=claude            # claude | openai
ANTHROPIC_API_KEY=sk-ant-xxx
ANTHROPIC_MODEL=claude-sonnet-4-6

# 文件限制
MAX_FILE_SIZE_MB=2048          # 最大 2GB
MAX_VIDEO_DURATION_SECONDS=14400  # 最长 4 小时
ALLOWED_FORMATS=mp4,mov,avi,mkv,mp3,wav,m4a,webm
```

## 处理流程

```
上传视频 → FFmpeg 提取音频 → Whisper 语音转文字
    → 说话人分离 → Claude API 智能摘要 → Jinja2 渲染输出 .md / .docx
```

任务状态机：`pending → processing → completed / failed / cancelled`

## License

MIT
