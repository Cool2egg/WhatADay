# WhatADay 项目最终开发方案

## 1. 文档目的

本文档是 WhatADay 项目的开发基准。后续代码、接口、数据库和前端实现都以本方案为准；如果开发过程中需要改变范围，应先更新本文档。

## 2. 项目定位

WhatADay 是一个运行在 Windows 用户桌面会话中的本地优先工作复盘 Agent。

系统定时读取当前前台应用和窗口信息，低频截取屏幕，通过视觉模型生成结构化活动事件；每天晚上由 LangChain4j Agent 查询活动和用户手动记录，生成并保存每日工作日报；用户通过 React 前端查看时间线、统计信息和日报。

项目重点不是持续录屏或商业级桌面客户端，而是完整演示以下工程链路：

```text
桌面采集 → 多模态理解 → 结构化持久化 → Agent Tool Calling → 自动日报 → 前端展示
```

## 3. 最终技术栈

### 后端

- Java 17
- Spring Boot
- LangChain4j
- SQLite + JdbcTemplate
- JNA
- Java AWT Robot
- Spring Scheduler
- Springdoc OpenAPI / Swagger

### 前端

- React
- TypeScript
- Vite
- Ant Design
- ECharts
- Axios

### 运行方式

- 开发环境：React 5173 端口，Spring Boot 8080 端口
- 演示环境：React 构建后复制到 Spring Boot `static` 目录，由后端统一提供
- 采集器要求运行在 Windows 用户桌面会话中
- Docker 仅作为可选的后端/API 演示方式，不承担桌面采集功能

明确不加入：Spring AI、多 Agent、RAG、向量数据库、MCP、Redis、消息队列、浏览器插件、Electron、WebSocket、OCR。

## 4. 系统架构

```text
React 前端
    ↓ REST API
Spring Boot Controller
    ↓
CollectorService ── JNA / AWT Robot
    ↓
CaptureObservation
    ↓
VisionActivityAnalyzer
    ↓
ActivityEvent ── SQLite

DailyReportAgent
    ├── queryActivityEvents(date)
    ├── queryUserNotes(date)
    └── saveDailyReport(report)
            ↓
       DailyReport ── SQLite

Spring Scheduler
    ↓ 每天 22:00
日报 Agent
```

视觉分析和日报 Agent 是两个独立流程：

```text
截图 + 窗口信息 → VisionActivityAnalyzer → ActivityEvent
ActivityEvent + UserNote → DailyReportAgent → DailyReport
```

采集器只负责确定性地获取数据，Agent 只负责理解、分类和总结，不负责持续监控或直接调用 Windows API。

## 5. 功能范围

### 5.1 桌面采集

- 每 30 秒读取一次前台窗口
- 获取应用名称、窗口标题和当前时间
- 每 2 分钟最多分析一张截图
- 第一次采集或窗口变化时允许触发新的观察
- 相同应用、窗口标题和活动类型的相邻事件可以合并
- 单个活动最多合并 30 分钟
- 视觉分析完成后删除原始截图
- 支持开始、停止、查看状态和立即采集

### 5.2 活动分析

视觉模型输入：

- 应用名称
- 窗口标题
- 当前时间
- 临时截图（如果满足截图条件）

模型输出必须映射为结构化对象：

```json
{
  "appName": "IntelliJ IDEA",
  "windowTitle": "CouponService.java",
  "type": "CODING",
  "description": "正在修改优惠券领取逻辑",
  "keywords": ["Java", "Spring Boot", "优惠券"],
  "confidence": 0.93
}
```

活动类型固定为：

```text
CODING
LEARNING
MEETING
BROWSING
ENTERTAINMENT
COMMUNICATION
OTHER
```

模型调用失败时，使用窗口信息生成降级事件，`source` 标记为 `WINDOW_FALLBACK`。

### 5.3 日报 Agent

每天 22:00 由 Spring Scheduler 触发：

```java
@Scheduled(cron = "0 0 22 * * *")
public void generateDailyReport() {
    dailyReportAgent.chat("请生成今天的工作日报");
}
```

Agent 只能通过以下工具访问数据：

```java
queryActivityEvents(LocalDate date)
queryUserNotes(LocalDate date)
saveDailyReport(DailyReport report)
```

工具边界：

- 只能查询指定日期
- 不能访问任意文件
- 不能修改活动记录和用户笔记
- 只能新增或更新指定日期的日报
- `report_date` 唯一，重复生成执行 upsert，保证幂等

日报结构：

```text
date
timeline
achievements
learning
distractions
nextActions
```

### 5.4 前端页面

#### 今日工作台

- 当前采集状态
- 开始/停止采集
- 今日采集时长
- 今日活动数量
- 活动类型占比图
- 最近活动列表

#### 时间线

- 按日期查询
- 按活动类型筛选
- 显示开始时间、结束时间和持续时长
- 显示应用、窗口、描述、关键词和置信度

#### 日报

- 查看今日及历史日报
- 生成今日日报
- 重新生成日报
- 展示时间线、成果、学习、分心事项和下一步行动

#### 手动记录

- 输入记录内容
- 选择记录时间
- 添加标签
- 保存并参与日报生成

前端使用轮询，不引入 WebSocket：

- 采集状态每 5 秒刷新
- 最近活动每 10 秒刷新

## 6. 数据库设计

### capture_observation

保存采集器观察到的事实：

```text
id
observed_at
app_name
window_title
analysis_status
analysis_error
created_at
```

### activity_event

保存视觉模型或降级逻辑生成的结构化活动：

```text
id
observation_id
start_time
end_time
app_name
window_title
type
description
keywords_json
confidence
source
created_at
```

### user_note

```text
id
note_time
content
tags_json
created_at
```

### daily_report

```text
report_date       UNIQUE / PRIMARY KEY
timeline_json
achievements_json
learning_json
distractions_json
next_actions_json
created_at
updated_at
```

SQLite 配置：

```sql
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;
```

第一版使用 `JdbcTemplate` 和 `schema.sql`，不使用 JPA。

## 7. API 设计

```http
POST /api/collector/start
POST /api/collector/stop
GET  /api/collector/status
POST /api/collector/capture-now

GET  /api/timeline/today
GET  /api/timeline?date=2026-09-09&type=CODING

POST /api/notes
GET  /api/notes?date=2026-09-09

POST /api/reports/today
POST /api/reports/{date}/generate
GET  /api/reports/{date}
```

所有 start、stop 和日报生成接口必须幂等。

## 8. 隐私设计

隐私过滤必须发生在截图之前：

```text
读取前台窗口
    ↓
判断进程是否在敏感应用黑名单
    ↓
敏感应用：不截图、不调用模型，只记录忽略状态
    ↓
普通应用：执行截图和分析
```

配置示例：

```yaml
collector:
  mode: mock
  excluded-processes:
    - WeChat.exe
    - Alipay.exe
    - 1Password.exe
    - KeePass.exe
```

必须满足：

- API 只监听 `127.0.0.1`
- 模型 API Key 从环境变量读取
- 截图存放临时目录
- 使用 `try/finally` 确保删除截图
- 默认只保存结构化活动，不保存完整屏幕
- 日志不输出截图内容和敏感信息
- `ActivityType` 使用 Java 枚举
- `confidence` 限制在 `0~1`
- 视觉模型输出失败时降级到窗口信息

## 9. Mock 模式

配置：

```yaml
whataday:
  collector:
    mode: mock
```

Mock 模式是正式功能的一部分，用于：

- 没有 Windows 桌面环境时开发
- 没有模型 API Key 时开发前端
- 模型调用失败时验证降级流程
- 面试现场快速演示

Mock 数据应覆盖编码、学习、会议、浏览、娱乐等活动类型，确保时间线、统计图和日报都有可展示内容。

## 10. 代码结构

### 后端

```text
src/main/java/com/example/whataday
├── collector
│   ├── ActiveWindowReader
│   ├── ScreenshotCapturer
│   ├── CaptureScheduler
│   ├── CollectorService
│   └── CollectorController
├── activity
│   ├── ActivityEvent
│   ├── ActivityType
│   ├── ActivityRepository
│   ├── ActivityService
│   ├── VisionActivityAnalyzer
│   └── TimelineController
├── note
│   ├── UserNote
│   ├── NoteRepository
│   └── NoteController
├── report
│   ├── DailyReport
│   ├── DailyReportAgent
│   ├── ReportRepository
│   ├── ReportService
│   └── ReportController
├── config
│   ├── LangChainConfig
│   ├── PrivacyConfig
│   └── SchedulerConfig
└── common
    ├── ApiResponse
    └── GlobalExceptionHandler
```

### 前端

```text
frontend/src
├── pages
│   ├── Dashboard
│   ├── Timeline
│   ├── Reports
│   └── Notes
├── components
│   ├── CollectorStatus
│   ├── ActivityTimeline
│   ├── ActivityStatistics
│   └── ReportCard
├── api
├── types
├── router
└── App.tsx
```

## 11. 开发顺序

1. 创建 Spring Boot 和 React 项目骨架
2. 创建 SQLite 表结构和 JdbcTemplate Repository
3. 实现 Mock 数据、REST API 和 Swagger
4. 实现前端工作台、时间线、日报和手动记录
5. 实现 JNA 前台窗口读取
6. 实现 Robot 截图
7. 接入视觉模型
8. 实现 LangChain4j Tool Calling 日报 Agent
9. 接入 Spring Scheduler
10. 补充隐私过滤、失败降级、构建脚本和 README

先完成 Mock 流程，再接入 Windows API 和模型，避免外部环境阻塞开发。

## 12. 验收标准

完成后必须能够：

- 在前端启动和停止采集
- 在 Mock 模式看到活动时间线
- 在 Windows 模式读取前台应用和窗口标题
- 成功截取临时屏幕图片并在分析后删除
- 将活动保存到 SQLite
- 通过 Swagger 调用所有核心 API
- 通过视觉模型生成结构化活动
- 通过 Agent Tool Calling 查询活动和笔记
- 保存可重复生成的日报
- 前端展示活动统计和日报内容
- 无模型 API Key 时仍可通过 Mock 模式完整演示

## 13. 面试表达

可以这样介绍：

> 我实现了一个基于屏幕活动感知的个人工作复盘 Agent。系统通过 JNA 获取前台应用和窗口信息，通过 AWT Robot 低频采集屏幕，再利用视觉模型将屏幕内容转换成结构化活动事件。每天晚上由 Spring Scheduler 触发 LangChain4j Agent，Agent 通过 Tool Calling 查询当天活动和手动记录，最终生成并保存日报。为了控制模型成本和隐私风险，我采用了低频采样、敏感应用过滤、分析后删除原始截图以及模型失败降级的设计。

## 14. 当前明确不做的内容

- 商业级桌面客户端
- 全天候视频录制
- 多用户和登录系统
- 多 Agent 协作
- RAG、向量数据库、MCP
- Redis、消息队列
- OCR
- 浏览器插件
- WebSocket 实时推送
- 复杂的跨平台支持

