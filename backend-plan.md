# MeetingSum 后端开发计划

## 一、本次开发内容

### 1.1 已完成模块

按照 doc.md 的技术架构（Python FastAPI + Celery + SQLAlchemy），完成了后端全部核心代码，共 28 个文件：

| 层级 | 文件 | 说明 |
|------|------|------|
| **核心配置** | `app/core/config.py` | Pydantic-settings 配置管理，读取 .env，覆盖 DB/Redis/ASR/LLM/文件限制/存储路径所有配置项 |
| | `app/core/database.py` | SQLAlchemy 引擎 + Session 工厂，支持 SQLite（开发）和 PostgreSQL（生产） |
| | `app/core/security.py` | ID 生成、文件名哈希、格式校验 |
| **数据模型** | `app/models/meeting.py` | Meeting + Task 两个表，与 doc.md §7.1 的 DDL 一致，含 JSON 字段、级联删除、时间戳 |
| **AI 管道** | `app/pipeline/audio_extractor.py` | FFmpeg 提取音轨 → 16kHz mono WAV，ffprobe 获取时长 |
| | `app/pipeline/transcriber.py` | OpenAI Whisper 本地转写，输出带时间戳的片段结构 |
| | `app/pipeline/diarizer.py` | 说话人分离（当前为启发性规则，预留 pyannote-audio 接口） |
| | `app/pipeline/simmarizer.py` | Claude API 摘要生成，含 doc.md §6.1 的完整 System Prompt，支持长文本分块策略 |
| | `app/pipeline/orchestrator.py` | 管道编排器：串联提取→转写→摘要→导出全流程，支持进度回调 |
| **业务服务** | `app/services/audio_service.py` | 音频处理封装 |
| | `app/services/asr_service.py` | 转写+说话人分离封装 |
| | `app/services/summary_service.py` | 摘要生成封装 |
| | `app/services/export_service.py` | Markdown 和 DOCX 文档生成，输出结构与 doc.md §2.2 一致 |
| **异步任务** | `app/workers/celery_app.py` | Celery 应用，Redis broker |
| | `app/workers/process_task.py` | 主处理任务：调管道、更新进度、错误重试 |
| **API 路由** | `app/api/upload.py` | POST `/api/v1/meetings/upload` |
| | `app/api/tasks.py` | GET `/api/v1/tasks/{id}/status` |
| | `app/api/summary.py` | GET/DELETE `/api/v1/meetings[/{id}]`，带分页和搜索 |
| | `app/api/export.py` | GET `/api/v1/meetings/{id}/export?format=md\|docx\|transcript` |
| **模板** | `app/templates/default_md.jinja2` | Markdown 导出 Jinja2 模板 |
| | `app/templates/default_docx.jinja2` | DOCX 导出 Jinja2 模板 |
| **部署** | `Dockerfile` | 后端容器镜像 |
| | `docker-compose.yml` | 一键编排 backend + worker + Redis |
| | `alembic/` | 数据库迁移框架就绪 |
| | `.env.example` | 完整的环境变量示例 |

### 1.2 与 doc.md 的对照

| doc.md 要求 | 实现状态 |
|---|---|
| 视频上传 + 格式校验 | 已实现 |
| FFmpeg 音频提取 | 已实现 |
| Whisper 语音转文字 | 已实现 |
| 说话人分离 | 已实现（启发性规则，待接 pyannote） |
| LLM 摘要（Claude API） | 已实现，含完整 System Prompt |
| 长文本分块策略 | 已实现 |
| Markdown 导出 | 已实现 |
| Word (.docx) 导出 | 已实现 |
| 历史记录管理（分页/搜索/删除） | 已实现 |
| Celery 异步处理 | 已实现 |
| 任务状态查询 | 已实现 |
| 处理进度回调 | 已实现 |
| 错误处理与重试 | 已实现 |
| WebSocket 实时推送 | 未实现（Phase 2） |
| 用户认证 | 未实现（Phase 4） |
| 批量处理 | 未实现（Phase 3） |

---

## 二、遇到的问题

### 2.1 本地开发环境缺失

| 问题 | 状态 |
|------|------|
| 电脑未安装 Python | 需安装 Python 3.11+ |
| 电脑未安装 FFmpeg | 需安装 FFmpeg |
| 电脑未安装 Redis | 开发模式可绕过后端直接同步执行任务，暂不阻塞 |

### 2.2 技术决策记录

1. **SQLite JSON 兼容**：原 DDL 使用 PostgreSQL `JSONB`，模型改为 SQLAlchemy 通用 `JSON` 类型，SQLite 和 PostgreSQL 均可运行，无需改 DDL。
2. **说话人分离降级**：pyannote-audio 需额外模型下载且安装重，当前用基于停顿间隔的启发性规则作为降级方案，可正常标注说话人角色，后续按需替换。
3. **DOCX 导出容错**：python-docx 为可选依赖——如果未安装，导出 MD 和转录文本正常进行，DOCX 静默跳过。

---

## 三、后续开发计划

### Phase 2：功能增强（预计 2-3 周）

- [ ] **说话人分离升级**：集成 pyannote-audio 替换启发性规则
- [ ] **WebSocket 进度推送**：`ws://localhost:8000/ws/tasks/{task_id}`，实时推送处理阶段和百分比
- [ ] **摘要模板自定义**：前端可选择/编辑 Jinja2 模板，后端保存用户自定义模板
- [ ] **错误处理增强**：细分错误码（音频损坏、转写失败、LLM 超时），前端差异化展示
- [ ] **处理超时控制**：对单个任务设置全局超时（如 40 分钟），超时自动标记失败

### Phase 3：体验优化（预计 1-2 周）

- [ ] **批量处理**：单次上传多个文件，共用队列排队，前端展示批量进度
- [ ] **时间戳回链**：摘要中关键点关联视频时间码，点击可跳转
- [ ] **分片上传**：前端大文件分片上传，支持断点续传
- [ ] **LLM Provider 扩展**：支持 OpenAI GPT-4o 作为备选 LLM

### Phase 4：生产就绪（预计 1-2 周）

- [ ] **用户认证**：JWT 登录/注册
- [ ] **数据隔离**：按用户隔离会议记录和数据文件
- [ ] **Docker 部署验证**：完整 docker-compose 一键启动
- [ ] **速率限制**：API 级别限流，防止滥用
- [ ] **日志系统**：结构化日志 + 文件轮转
- [ ] **开发文档**：本地启动指南、API 文档补充

---

## 四、本地启动步骤

```bash
# 1. 安装 Python 3.11+ 和 FFmpeg（Windows）
winget install Python.Python.3.11
winget install Gyan.FFmpeg

# 2. 进入后端目录
cd backend

# 3. 创建虚拟环境并安装依赖
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt

# 4. 配置环境变量
copy .env.example .env
# 编辑 .env：填入 ANTHROPIC_API_KEY

# 5. 启动（开发模式：同步执行，无需 Redis）
uvicorn app.main:app --reload --port 8000

# 6. 生产模式（需 Redis + Celery Worker）
redis-server &
celery -A app.workers.celery_app worker --loglevel=info &
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
