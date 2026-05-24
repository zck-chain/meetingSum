# MeetingSum 前端开发文档

## 1. 项目概述

MeetingSum 是一款会议/学习视频智能总结工具的前端应用。用户上传视频或音频文件后，系统自动完成语音转文字、智能摘要生成、关键要点提取，并支持导出结构化文档。

- **应用名称**: MeetingSum
- **当前版本**: 0.1.0
- **项目路径**: `frontend/`
- **开发服务器**: `npm run dev` → `http://localhost:5173/`

---

## 2. 技术栈

| 类别 | 选型 | 版本 |
|------|------|------|
| 构建工具 | Vite | ^6.0 |
| 框架 | React | ^19.0 |
| 语言 | TypeScript | ^5.7 |
| UI 组件库 | Ant Design | ^5.22 |
| CSS 框架 | Tailwind CSS | ^4.0 |
| 状态管理 | Zustand | ^5.0 |
| 路由 | React Router DOM | ^7.0 |
| HTTP 客户端 | Axios | ^1.7 |
| API Mock | MSW (Mock Service Worker) | ^2.0 |
| 文件上传 | react-dropzone | ^14.3 |
| 图标 | @ant-design/icons | ^5.5 |

---

## 3. 项目结构

```
frontend/
├── public/
│   └── mockServiceWorker.js        # MSW Service Worker
├── src/
│   ├── api/                        # API 请求封装
│   │   ├── client.ts               # Axios 实例 + 拦截器
│   │   ├── meetings.ts             # 会议 CRUD 接口
│   │   ├── tasks.ts                # 任务状态查询接口
│   │   └── export.ts               # 导出下载接口
│   │
│   ├── components/                 # UI 组件
│   │   ├── Layout/
│   │   │   └── AppLayout.tsx       # 全局布局（Sider + Header + Content）
│   │   ├── Uploader/
│   │   │   └── FileUploader.tsx    # 拖拽上传组件
│   │   ├── HistoryList/
│   │   │   └── HistoryTable.tsx    # 历史记录表格
│   │   ├── SummaryViewer/
│   │   │   └── SummaryContent.tsx  # 结构化摘要展示
│   │   └── ExportPanel/
│   │       └── ExportActions.tsx   # 导出操作面板
│   │
│   ├── pages/                      # 页面
│   │   ├── Home.tsx                # 首页（上传 + 最近会议）
│   │   ├── History.tsx             # 历史记录页
│   │   ├── Summary.tsx             # 摘要详情页
│   │   └── Settings.tsx            # 设置页
│   │
│   ├── stores/                     # Zustand 状态管理
│   │   ├── meetingStore.ts         # 会议状态（列表/详情/CRUD）
│   │   ├── taskStore.ts            # 任务状态（进度轮询）
│   │   └── settingsStore.ts        # 设置状态（本地持久化）
│   │
│   ├── types/                      # TypeScript 类型定义
│   │   ├── meeting.ts              # Meeting, SummaryData, KeyPoint 等
│   │   ├── task.ts                 # Task, TaskStage, ProgressEvent
│   │   └── settings.ts             # Settings, AsrProvider, LlmProvider
│   │
│   ├── mocks/                      # MSW Mock 层
│   │   ├── data.ts                 # 7 条模拟会议数据 + 进度模拟算法
│   │   ├── browser.ts              # MSW browser 初始化
│   │   └── handlers/
│   │       └── meetingHandlers.ts  # 7 个 REST 端点的 mock handler
│   │
│   ├── utils/                      # 工具函数
│   │   ├── format.ts               # 时长/文件大小格式化
│   │   └── validators.ts           # 文件格式/大小校验
│   │
│   ├── App.tsx                     # 路由 + Ant Design ConfigProvider
│   ├── main.tsx                    # 应用入口（MSW 启动 + render）
│   └── index.css                   # Tailwind CSS 入口
│
├── package.json
├── vite.config.ts                  # Vite 配置（路径别名 + Tailwind 插件）
├── tsconfig.json
└── tsconfig.app.json
```

---

## 4. 已完成功能（Phase 1 MVP + 完整页面结构）

### 4.1 页面

| 页面 | 路由 | 功能描述 | 状态 |
|------|------|----------|------|
| Home | `/` | 文件上传（拖拽/点击）、上传进度展示、处理状态实时轮询、最近会议卡片列表、完成/失败状态提示 | ✅ 完成 |
| History | `/history` | 会议列表（分页）、关键词搜索、状态标签筛选、删除确认、跳转摘要详情 | ✅ 完成 |
| Summary | `/summary/:id` | 结构化摘要展示（基本信息/摘要/讨论点/决策/行动项）、完整转录折叠面板、多格式导出下载、Markdown 复制 | ✅ 完成 |
| Settings | `/settings` | ASR 提供商选择、LLM 提供商选择、摘要模板预留入口、系统信息展示、设置 localStorage 持久化 | ✅ 完成 |

### 4.2 组件

| 组件 | 文件 | 功能 | 状态 |
|------|------|------|------|
| AppLayout | `components/Layout/AppLayout.tsx` | 侧边栏折叠导航、面包屑 Header、Footer 版本号、`<Outlet />` 子路由渲染 | ✅ 完成 |
| FileUploader | `components/Uploader/FileUploader.tsx` | react-dropzone 拖拽上传、格式/大小前端校验、上传进度条、成功/失败消息提示 | ✅ 完成 |
| HistoryTable | `components/HistoryList/HistoryTable.tsx` | Ant Design Table、搜索框、分页、状态 Tag 着色、删除二次确认、查看摘要跳转 | ✅ 完成 |
| SummaryContent | `components/SummaryViewer/SummaryContent.tsx` | Descriptions 基本信息、Timeline 讨论点（带重要性着色）、Checkbox 决策记录、Table 行动项、Collapse 转录文本 | ✅ 完成 |
| ExportActions | `components/ExportPanel/ExportActions.tsx` | Radio.Group 格式选择（md/docx/pdf/txt）、下载按钮、复制 Markdown 按钮 | ✅ 完成 |

### 4.3 状态管理

| Store | 文件 | 职责 | 状态 |
|-------|------|------|------|
| meetingStore | `stores/meetingStore.ts` | meetings 列表、currentMeeting 详情、fetchMeetings（分页+搜索）、fetchMeeting、uploadMeeting、deleteMeeting | ✅ 完成 |
| taskStore | `stores/taskStore.ts` | 任务 ID、stage/progress/status、startPolling（1 秒轮询）、stopPolling、自动完成检测 | ✅ 完成 |
| settingsStore | `stores/settingsStore.ts` | asrProvider/llmProvider、zustand persist 中间件 → localStorage | ✅ 完成 |

### 4.4 API Mock 层

| 方法 | 路径 | Mock 行为 | 状态 |
|------|------|-----------|------|
| GET | `/api/v1/meetings` | 返回分页列表，支持 `?search=&page=&page_size=` 查询 | ✅ 完成 |
| GET | `/api/v1/meetings/:id` | 返回单条会议详情 | ✅ 完成 |
| POST | `/api/v1/meetings/upload` | 创建新会议 + 任务，返回 `{ meeting, task_id }` | ✅ 完成 |
| DELETE | `/api/v1/meetings/:id` | 删除会议，返回 `{ success: true }` | ✅ 完成 |
| GET | `/api/v1/meetings/:id/export?format=` | 返回模拟文件 Blob（md/docx） | ✅ 完成 |
| GET | `/api/v1/tasks/:id/status` | 模拟渐进式进度（extracting_audio → transcribing → summarizing → exporting），约 100 秒完成 | ✅ 完成 |

### 4.5 类型定义

- **Meeting**: id, title, original_file, original_format, duration_seconds, file_size_bytes, status, transcript_text, summary_json, error_message, created_at, updated_at
- **SummaryData**: title, summary, key_points[], decisions[], action_items[], tags[]
- **KeyPoint**: topic, content, importance (high/medium/low)
- **Decision**: content, proposer
- **ActionItem**: content, assignee, deadline
- **Task**: id, meeting_id, stage, progress, status, result_path, created_at, completed_at

---

## 5. 设计决策

1. **Ant Design 为主 UI 库**：利用 antd 的 Layout/Menu/Table/Form/Modal/Progress/Tag/Collapse/Descriptions 组件快速构建界面，Tailwind CSS 仅用于微调间距和自定义样式
2. **MSW 在 network 层 mock**：所有 API 调用走真实 HTTP 请求，MSW 在 Service Worker 层拦截，切换到真实后端只需关闭 MSW
3. **Zustand 分层 store**：meeting、task、settings 三个独立 store，避免巨型 store 导致的性能问题
4. **设置本地持久化**：settingsStore 使用 zustand/middleware persist 写入 localStorage
5. **任务进度模拟**：上传后 taskStore 启动 1 秒间隔轮询，MSW handler 基于 `Date.now()` 计算已用时间并返回渐进式 progress

---

## 6. 当前限制

- 所有数据来自 MSW mock，刷新后新增的会议丢失（mock 数据在内存中）
- 上传的文件不会真实存储，上传进度为模拟值
- 导出下载的文件内容为占位文本，非真实摘要
- 转录文本在 mock 数据中均为 null，摘要页显示"转录文本暂未存储"
- 未实现用户认证，无登录/注册功能
- 未配置 Electron 桌面端壳
- 未实现 WebSocket 实时进度推送（当前使用轮询）

---

## 7. 后续开发计划

### Phase 2：功能增强（预计 2-3 周）

| 任务 | 描述 | 涉及文件（新建/修改） | 优先级 |
|------|------|----------------------|--------|
| WebSocket 实时进度 | 替换轮询为 WebSocket 连接 `ws://localhost:8000/ws/tasks/{task_id}`，监听 progress/complete/error 事件 | `stores/taskStore.ts`（修改）、`api/ws.ts`（新建） | P0 |
| 说话人分离 UI | 在 SummaryContent 中增加说话人标识展示，转录文本按说话人分段显示 | `components/SummaryViewer/SummaryContent.tsx`（修改） | P1 |
| Word 导出完善 | 补充 `.docx` 导出的真实模板渲染，确保格式完整 | `api/export.ts`（修改）、后端依赖 | P1 |
| 摘要模板自定义 | Settings 页实现模板编辑器（Monaco Editor 或 TextArea），用户可自定义输出模板变量 | `pages/Settings.tsx`（修改）、`components/Settings/TemplateEditor.tsx`（新建） | P1 |
| 错误处理与重试 | 失败任务的重试按钮、错误详情弹窗、全局错误边界 | `components/HistoryList/HistoryTable.tsx`（修改）、`components/ErrorBoundary.tsx`（新建） | P1 |
| 批量上传 | FileUploader 支持 `multiple={true}`，多文件排队上传，批量处理状态面板 | `components/Uploader/FileUploader.tsx`（修改）、`components/Uploader/BatchProgress.tsx`（新建） | P2 |

### Phase 3：体验优化（预计 1-2 周）

| 任务 | 描述 | 涉及文件（新建/修改） | 优先级 |
|------|------|----------------------|--------|
| Electron 桌面端 | 创建 `electron/main.ts` 主进程，配置窗口、系统托盘、本地文件关联 | `electron/main.ts`（新建）、`package.json`（修改） | P0 |
| 拖拽上传增强 | 支持文件夹拖拽、粘贴板粘贴、URL 链接导入 | `components/Uploader/FileUploader.tsx`（修改） | P1 |
| 时间戳回链 | 摘要中关键词/讨论点关联视频时间戳，点击可跳转（需视频播放器集成） | `components/SummaryViewer/SummaryContent.tsx`（修改）、`components/VideoPlayer/`（新建） | P1 |
| 多语言支持 | 引入 `react-i18next`，提取中/英文文案，语言切换入口 | 全局修改，`locales/`（新建） | P2 |
| 暗色模式 | 适配 Ant Design `theme.algorithm.darkAlgorithm`，添加主题切换开关 | `App.tsx`（修改）、`stores/settingsStore.ts`（修改） | P2 |
| 快捷键支持 | 全局快捷键（Ctrl+U 上传、Ctrl+F 搜索等） | `utils/shortcuts.ts`（新建） | P2 |

### Phase 4：生产就绪（预计 1-2 周）

| 任务 | 描述 | 涉及文件（新建/修改） | 优先级 |
|------|------|----------------------|--------|
| 用户认证 UI | 登录/注册页面、Token 管理、路由守卫、Axios 拦截器注入 Authorization header | `pages/Login.tsx`（新建）、`pages/Register.tsx`（新建）、`api/client.ts`（修改）、`stores/authStore.ts`（新建） | P0 |
| 大文件分片上传 | 文件分片上传 + 断点续传 UI 进度展示 | `components/Uploader/ChunkedUploader.tsx`（新建）、`api/upload.ts`（新建） | P1 |
| 性能优化 | 路由懒加载（React.lazy + Suspense）、虚拟列表（react-window 处理大量历史记录）、Ant Design 按需引入 | `App.tsx`（修改）、`vite.config.ts`（修改） | P1 |
| Docker 部署配置 | 前端 `Dockerfile`（多阶段构建 nginx 镜像）、`nginx.conf` | `Dockerfile`（新建）、`nginx.conf`（新建） | P1 |
| 使用文档页面 | 应用内帮助中心/引导页 | `pages/Help.tsx`（新建） | P2 |
| 埋点与监控 | 接入前端监控 SDK（错误追踪、性能指标、用户行为） | `main.tsx`（修改）、`utils/analytics.ts`（新建） | P2 |

---

## 8. 与 doc.md 的对照

| doc.md 章节 | 内容 | 前端覆盖状态 |
|-------------|------|-------------|
| 2.1 核心功能 | 视频导入、语音识别、说话人分离、智能摘要、要点提取、文档导出、时间戳标注 | 上传 ✅ / ASR 状态展示 ✅ / 说话人 UI 预留 ⏳ / 摘要展示 ✅ / 要点提取展示 ✅ / 导出 ✅ / 时间戳 ⏳ |
| 2.2 输出结构 | Markdown 结构化模板 | 完全对齐（基本信息/摘要/讨论点/决策/行动项） ✅ |
| 2.3 辅助功能 | 历史记录、模板自定义、批量处理、导出格式选择 | 历史 ✅ / 模板 ⏳ / 批量 ⏳ / 导出格式 ✅ |
| 5.1 REST API | 7 个接口 | 全部通过 MSW mock ✅ |
| 5.3 WebSocket | 实时进度推送 | 当前使用轮询，WebSocket ⏳ |
| 8.1 Phase 1 MVP | 基础 Web UI（上传→等待→查看/下载） | ✅ 完成 |
| 8.1 Phase 2 | 说话人分离、Word 导出、进度推送、模板、历史管理 | ⏳ 待开发 |
| 8.1 Phase 3 | Electron、拖拽增强、时间戳、国际化 | ⏳ 待开发 |
| 8.1 Phase 4 | 认证、加密、分片上传、Docker | ⏳ 待开发 |

---

## 9. 开发命令

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 类型检查
npx tsc --noEmit

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

---

## 10. 切换到真实后端

当后端 API 就绪后，只需做以下改动即可从 MSW mock 切换到真实 API：

1. **关闭 MSW**：删除 `src/main.tsx` 中的 MSW 启动代码块
2. **配置 API 地址**：在 `src/api/client.ts` 中将 `baseURL` 改为真实后端地址（或通过环境变量 `VITE_API_BASE_URL` 配置）
3. **无需改动任何业务代码**：所有 API 调用、Store、组件逻辑完全不变
