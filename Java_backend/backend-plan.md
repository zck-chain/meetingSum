# MeetingSum 后端开发文档

## 一、项目概述

MeetingSum 后端提供两套实现，API 路径和响应格式完全一致，前端无需感知后端语言。

| 实现 | 技术栈 | 目录 | 端口 |
|------|--------|------|------|
| **Java 版** | Java 17 + Spring Boot 3.3 + Spring Data JPA + LangChain4j | `Java_backend/` | 8080 |
| **Python 版** | Python 3.11 + FastAPI + Celery + SQLAlchemy | `backend/` | 8000 |

---

## 二、Java 后端

### 2.1 技术栈

| 类别 | 选型 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.3.5 | REST API + 内嵌 Tomcat |
| ORM | Spring Data JPA + Hibernate | H2 文件数据库，DDL 自动建表 |
| 数据库 | H2 (file mode) | `./data/meetingsum`，H2 Console 在 `/h2-console` |
| LLM 框架 | LangChain4j 0.35 | 封装 DeepSeek / Claude API 调用 |
| 文档导出 | Apache POI 5.3 | DOCX 生成 |
| 异步处理 | Spring @Async | 线程池异步执行 AI 管道 |
| 语音识别 | OpenAI Whisper (Python 子进程) | 通过 `ProcessBuilder` 调用 Python 脚本 |

### 2.2 已完成的模块（49 个源文件）

| 层级 | 文件 | 说明 |
|------|------|------|
| **配置** | `config/AppProperties.java` | `@ConfigurationProperties(prefix="app")`，映射 application.yml |
| | `config/AsyncConfig.java` | 线程池配置（pipelineExecutor） |
| | `config/CorsConfig.java` | CORS 白名单（localhost:5173 / localhost:3000） |
| | `application.yml` | 全部配置项（DB / ASR / LLM / FFmpeg / 文件限制） |
| **控制器** | `controller/MeetingController.java` | POST upload / GET 列表 / GET 详情 / DELETE |
| | `controller/TaskController.java` | GET 任务状态 |
| | `controller/ExportController.java` | GET 导出下载 |
| | `controller/HealthController.java` | GET 健康检查 |
| | `controller/GlobalExceptionHandler.java` | 统一异常 → JSON 错误响应 |
| | `controller/PayloadTooLargeException.java` | 文件过大专用异常 |
| **实体** | `entity/Meeting.java` | JPA Entity，CLOB 存储 transcript/summary |
| | `entity/Task.java` | JPA Entity，关联 Meeting |
| **DTO** | `dto/UploadResponse.java` | 上传响应 `{meetingId, taskId, filename, status}` |
| | `dto/MeetingListResponse.java` | 分页列表 `{items, total, page, page_size}` |
| | `dto/MeetingDetailResponse.java` | 详情含 summary / transcript |
| | `dto/TaskStatusResponse.java` | 任务进度，含 `error_code` / `error_detail` |
| | `dto/SummaryData.java` | 结构化摘要 POJO |
| | `dto/DeleteResponse.java`、`dto/HealthResponse.java` | 通用响应 |
| **枚举** | `enums/MeetingStatus.java` | PENDING / PROCESSING / COMPLETED / FAILED |
| | `enums/TaskStage.java` | EXTRACTING_AUDIO / TRANSCRIBING / SUMMARIZING / EXPORTING |
| | `enums/TaskStatus.java` | PENDING / PROCESSING / COMPLETED / FAILED |
| **管道** | `pipeline/AudioExtractor.java` | FFmpeg 提取音频 + ffprobe 获取时长 |
| | `pipeline/TranscriberService.java` | 调用 Python Whisper 脚本，注入 ffmpeg PATH |
| | `pipeline/Diarizer.java` | 启发性规则说话人分离（停顿 > 2s 切换说话人） |
| | `pipeline/SummarizerService.java` | LangChain4j 调用 DeepSeek/Claude，含 System Prompt + 长文本分块 |
| | `pipeline/ExportService.java` | Markdown 文本 + Apache POI DOCX 导出 |
| **服务** | `service/MeetingService.java` | 会议 CRUD + @Transactional |
| | `service/TaskService.java` | 任务管理 + 进度更新 |
| | `service/PipelineService.java` | @Async 管道编排，4 阶段（提取→转写→总结→导出） |
| | `service/FileStorageService.java` | 文件存储管理 |
| **仓库** | `repository/MeetingRepository.java` | JPA Repository + 自定义查询 |
| | `repository/TaskRepository.java` | JPA Repository |
| **工具** | `util/IdGenerator.java` | 32 位随机 ID 生成 |
| | `util/FileValidationUtils.java` | 格式白名单校验 |
| **脚本** | `whisper_transcribe.py` | Whisper 转写桥接脚本，stdout 输出 JSON |
| | `pyannote_diarize.py` | pyannote-audio 说话人分离桥接脚本 |
| **WebSocket** | `config/WebSocketConfig.java` | 注册 `/ws/tasks/{taskId}` WebSocket 端点 |
| | `websocket/TaskProgressWebSocketHandler.java` | WebSocket 连接/断开管理 |
| | `websocket/WebSocketSessionManager.java` | 按 taskId 管理会话，广播进度/完成/失败消息 |
| **错误处理** | `enums/ErrorCode.java` | 13 个结构化错误码，按阶段前缀分组 |
| | `pipeline/PipelineException.java` | 携带 ErrorCode 的自定义运行时异常 |

### 2.3 与 doc.md 的对照

| doc.md 要求 | Java 实现状态 |
|---|---|
| POST /api/v1/meetings/upload | ✅ 已实现（MultipartFile，返回 meeting + task_id） |
| GET /api/v1/meetings（分页+搜索） | ✅ 已实现（JPA Pageable + Specification） |
| GET /api/v1/meetings/{id} | ✅ 已实现 |
| DELETE /api/v1/meetings/{id} | ✅ 已实现 |
| GET /api/v1/tasks/{id}/status | ✅ 已实现（含 stage + progress + status） |
| GET /api/v1/meetings/{id}/export | ✅ 已实现（md / docx / transcript） |
| FFmpeg 音频提取 | ✅ 已实现（可配置路径，不依赖系统 PATH） |
| Whisper 语音转文字 | ✅ 已实现（Python 子进程，UTF-8 编码处理） |
| 说话人分离 | ✅ 已实现（启发性规则） |
| LLM 摘要 | ✅ 已实现（DeepSeek + Claude，LangChain4j 封装） |
| 长文本分块策略 | ✅ 已实现 |
| Markdown 导出 | ✅ 已实现 |
| Word (.docx) 导出 | ✅ 已实现（Apache POI） |
| 异步处理 | ✅ 已实现（Spring @Async） |
| 任务进度回调 | ✅ 已实现 |
| 错误处理 | ✅ 已实现（GlobalExceptionHandler） |
| WebSocket 实时推送 | ✅ 已实现（Phase 2） |
| 用户认证 | ❌ 未实现 |

---

## 三、近期变更记录

### 3.3 Phase 2 功能增强（2026-06-01）

| 功能 | 说明 |
|------|------|
| **说话人分离升级** | 新增 `pyannote_diarize.py` Python 桥接脚本，支持 pyannote-audio ML 说话人分离。`Diarizer.java` 新增 `assignSpeakersWithAudio()` 方法，失败自动降级回启发式规则。配置项 `app.pyannote-*`。 |
| **WebSocket 进度推送** | 新增 `WebSocketConfig`、`TaskProgressWebSocketHandler`、`WebSocketSessionManager`。`/ws/tasks/{taskId}` 端点实时推送进度/完成/失败事件。`PipelineService.emitProgress()` 并行推送 WebSocket 消息。前端 `vite.config.ts` 添加 `/ws` 代理。 |
| **摘要模板自定义** | `PUT /api/v1/meetings/{id}/template` 新增端点。三级优先级：会议级 > YAML `app.summary-template` > 硬编码默认。`Meeting` 实体新增 `customSummaryTemplate` 字段。 |
| **错误处理增强** | 新增 `ErrorCode` 枚举（13 个错误码）和 `PipelineException`。`PipelineService.runPipeline()` 拆分为每阶段独立 try-catch。`Task` 实体新增 `errorCode`/`errorMessage` 字段。`TaskStatusResponse` 新增 `error_code`/`error_detail` 字段。 |
| **超时控制** | 新增 `AppProperties.TimeoutConfig` 嵌套配置类。`application.yml` 新增 `app.timeout.*` 可配置每阶段超时。`PipelineService` 新增全局超时检查。`AudioExtractor`/`TranscriberService`/`SummarizerService` 使用配置的超时替代硬编码值。 |

### 3.2 关键设计决策

1. **FFmpeg 路径可配置**：不依赖系统 PATH，通过 `application.yml` 显式指定完整路径，Windows 和 Linux 均可适配
2. **Whisper 通过 Python 子进程调用**：Java 侧用 `ProcessBuilder` 启动独立 Python 进程，避免 JNI 复杂度
3. **编码兼容**：中英文 Windows 环境下保持平台默认编码一致性，避免强制 UTF-8 导致的性能或兼容问题
4. **说话人分离双轨制**：pyannote-audio 提供 ML 级别的说话人分离，失败时自动降级为基于停顿间隔（> 2s）的启发性规则
5. **H2 文件数据库**：免安装，数据文件在 `./data/` 目录，支持 Hibernate `ddl-auto: update` 自动建表

### 3.1 联调修复（2026-05-28）

| 问题 | 根因 | 修复 |
|------|------|------|
| CORS 403 | CorsConfig 只允许 localhost:3000 | 添加 `localhost:5173`、`127.0.0.1:5173` |
| FFmpeg 找不到 | 硬编码 `ffmpeg.cmd`，Windows 安装无此文件 | 改为可配置路径 `application.yml` → `ffmpeg-path` / `ffprobe-path` |
| Whisper 找不到 ffmpeg | Python 子进程未继承 PATH | `ProcessBuilder.environment().put("PATH", ...)` 注入 ffmpeg 目录 |
| JSON 解析失败 | Whisper `transcribe()` 往 stdout 打印 "Detected..." | Python 脚本 `redirect_stdout(devnull)` 屏蔽干扰输出 |
| 中文乱码 | Windows Python stdout 默认 GBK，Java 侧读取不一致 | 回退为 Java `new String(bytes)` 默认编码与 Python GBK 一致 |

---

## 四、Python 后端

Python 版后端位于 `backend/` 目录，基于 FastAPI + Celery + SQLAlchemy，核心模块包括：

| 层级 | 说明 |
|------|------|
| `app/core/` | Pydantic-settings 配置、SQLAlchemy 引擎、安全工具 |
| `app/models/` | Meeting + Task 数据模型 |
| `app/pipeline/` | FFmpeg 提取 → Whisper 转写 → 说话人分离 → Claude 摘要 → Jinja2 导出 |
| `app/workers/` | Celery 异步任务 + Redis broker |
| `app/api/` | REST 路由（upload / tasks / summary / export） |

详细文档见 `backend/backend-plan.md`。

---

## 五、API 概览

两套后端 API 路径和响应格式完全一致：

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/meetings/upload` | 上传音视频文件（multipart/form-data） |
| `GET` | `/api/v1/meetings` | 分页列表，支持 `?search=&page=&page_size=&status=` |
| `GET` | `/api/v1/meetings/{id}` | 会议详情（含 summary_json + transcript_text） |
| `DELETE` | `/api/v1/meetings/{id}` | 删除会议及关联文件 |
| `GET` | `/api/v1/tasks/{id}/status` | 任务进度 `{stage, progress, status}` |
| `PUT` | `/api/v1/meetings/{id}/template` | 设置摘要模板（请求体 `{"template": "..."}` ) |
| `GET` | `/api/v1/meetings/{id}/export?format=md` | 导出 Markdown |
| `GET` | `/api/v1/meetings/{id}/export?format=docx` | 导出 Word |
| `WS` | `/ws/tasks/{id}` | WebSocket 实时进度推送 |

---

## 六、后续开发计划

### Phase 2：功能增强

- [x] **说话人分离升级**：集成 pyannote-audio 替换启发性规则（✅ 2026-06-01）
- [x] **WebSocket 进度推送**：实时推送处理阶段和百分比，前端替换轮询（✅ 2026-06-01）
- [x] **摘要模板自定义**：用户可自定义 System Prompt 模板（✅ 2026-06-01）
- [x] **错误处理增强**：细分错误码（音频损坏、转写失败、LLM 超时），前端差异化展示（✅ 2026-06-01）
- [x] **处理超时控制**：全局超时 + 各阶段超时独立配置（✅ 2026-06-01）

### Phase 3：体验优化

- [ ] **批量处理**：单次上传多个文件，队列排队，前端批量进度面板
- [ ] **时间戳回链**：摘要关键点关联视频时间码
- [ ] **分片上传**：大文件分片上传 + 断点续传
- [ ] **LLM Provider 扩展**：支持更多 LLM（GPT-4o / 文心一言 / 通义千问）

### Phase 4：生产就绪

- [ ] **用户认证**：JWT 登录/注册（Java 版 Spring Security）
- [ ] **数据隔离**：按用户隔离会议记录
- [ ] **Docker 部署**：完整 docker-compose（Java 版 + Python 版两套编排）
- [ ] **速率限制 + 日志系统**

---

## 七、本地启动（Java 版）

```bash
# 1. 安装依赖
#    - JDK 17+
#    - Maven 3.6+
#    - Python 3.11+（Whisper 依赖）
#    - FFmpeg（音频处理 + Whisper 内部调用）

# 2. 安装 Python Whisper
pip install openai-whisper

# 3. 进入 Java 后端目录
cd Java_backend

# 4. 配置
#    编辑 src/main/resources/application.yml：
#    - ffmpeg-path: 你的 FFmpeg 完整路径
#    - ffprobe-path: 你的 ffprobe 完整路径
#    - deepseek-api-key: 你的 API Key

# 5. 启动
mvn spring-boot:run

# API: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

## 八、本地启动（Python 版）

```bash
cd backend
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env      # 编辑 .env 填入 API Key

# 开发模式（同步，无需 Redis）
uvicorn app.main:app --reload --port 8000

# 生产模式（需 Redis）
redis-server &
celery -A app.workers.celery_app worker --loglevel=info &
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
