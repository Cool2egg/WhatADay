# WhatADay 开发与 GitHub 维护计划

> 本文档是**推进节奏**的基准：怎么把项目分成多次完成、每次交付什么、如何维护 GitHub。
> 功能与技术细节以 `PROJECT_PLAN.md` 为准（那份管「做什么」，本文档管「怎么分次做」）。
> 两者冲突时，功能范围以 `PROJECT_PLAN.md` 为准。

---

## 1. 项目定位与目标

WhatADay 是简历上的**副项目**（主项目为优惠券项目）。定位：本地优先的工作复盘 Agent，演示一条完整工程链路。

```text
桌面采集 → 多模态理解 → 结构化持久化 → Agent Tool Calling → 自动日报 → 前端展示
```

作为简历项目，成功标准只有三条：

1. **能跑**：核心链路端到端可演示，Mock 模式无环境也能完整跑通。
2. **有亮点**：日报 Agent（LangChain4j Tool Calling）+ 视觉模型转结构化事件，能在面试里讲 5 分钟。
3. **门面干净**：GitHub 主页 README 有截图、有架构图、提交历史清晰，链接可直接放简历。

**优先级排序：README 截图与亮点 > 提交历史干净 > CI 徽章 > 其他一切。**

---

## 2. 推进策略（总原则）

- **Mock 先行**：M0–M3 用 Mock 数据跑通全链路，不依赖 Windows 环境与模型 API Key。
- **每个里程碑都是「完整可交付状态」**：能编译、能运行、已 commit、打了 tag。绝不把半成品留在现场。
- **一次 session 推进一个里程碑**（复杂里程碑可拆成两次，见各节）。
- **日报链路是主线**，任何范围压缩都优先砍别的，不砍日报。前端一完成立刻截图留存。
- **改范围先改文档**：`PROJECT_PLAN.md` 或本文档。

---

## 3. 里程碑总览

| 里程碑 | 内容 | 对应 PLAN 步骤 | Tag | 验收标准 |
|---|---|---|---|---|
| **M0** | 仓库初始化 + 前后端骨架 + 规范文件 | 1 | v0.1.0 | 前后端都能 build |
| **M1** | SQLite 表结构 + JdbcTemplate Repository | 2 | v0.2.0 | schema 建表成功，Repository 单测通过 |
| **M2** | Mock 数据 + REST API + Swagger | 3 | v0.3.0 | Swagger 可调通所有核心 API |
| **M3** | 前端四页 + 轮询 | 4 | v0.4.0 | 前端看到 Mock 时间线/统计/日报 |
| **M4** | JNA 前台窗口 + Robot 截图 | 5–6 | v0.5.0 | Windows 下读到真实窗口并截图 |
| **M5** | 视觉模型接入 + 降级 | 7 | v0.6.0 | 生成结构化 ActivityEvent |
| **M6** | Agent 日报 + Scheduler + 收尾 | 8–10 | v1.0.0 | 日报可生成、可重复生成，README 完成 |

---

## 4. 各里程碑详细任务

### M0 —— 仓库初始化 + 骨架（tag v0.1.0）

**目标**：项目一出生就是「正规」的，前后端骨架能 build。

任务：
- `git init`，配置提交身份；创建 GitHub 仓库并关联 remote
- 编写 `.gitignore`（Java/Gradle/Maven + Node 标准模板，排除 `target/`、`build/`、`node_modules/`、截图临时目录、`.env`）
- `README.md` 初版（定位 + 架构图占位 + 技术栈 + Roadmap）
- `LICENSE`（MIT）
- 后端骨架：Spring Boot 工程、包结构按 PLAN 第 10 节建好空目录
- 前端骨架：React + TypeScript + Vite + Ant Design + ECharts
- `.github/workflows/ci.yml`：后端 build（+ 前端 build）

交付物：能 `build` 的双端工程 + 规范文件齐全。

提交示例：
```
chore: 初始化项目仓库与 .gitignore
feat: 搭建 Spring Boot 后端骨架
feat: 搭建 React + Vite 前端骨架
docs: 编写 README 初版
chore: 添加 GitHub Actions 构建流水线
```

> 开工前需用户确认：① GitHub 署名（用户名 + 邮箱）② 构建工具 Maven/Gradle。

---

### M1 —— 数据层（tag v0.2.0）

**目标**：四张表建好，Repository 可用。

任务（对应 PLAN 第 6 节）：
- `schema.sql`：`capture_observation`、`activity_event`、`user_note`、`daily_report`
- SQLite 配置：`PRAGMA journal_mode=WAL`、`PRAGMA busy_timeout=5000`
- 实体类 + JdbcTemplate Repository（不使用 JPA）
- `daily_report.report_date` 设 UNIQUE
- 每个 Repository 配最小单测

交付物：schema + Repository + 单测。

提交示例：
```
feat: 创建 SQLite 表结构与 schema.sql
feat: 实现 ActivityRepository
test: 补充每日报表 Repository 单元测试
```

---

### M2 —— Mock 数据 + REST API + Swagger（tag v0.3.0）

**目标**：不依赖任何外部环境，接口全部可调通。

任务（对应 PLAN 第 7、9 节）：
- Mock 数据生成器：覆盖 CODING / LEARNING / MEETING / BROWSING / ENTERTAINMENT / COMMUNICATION / OTHER 七种类型
- 实现全部 REST API：collector / timeline / notes / reports
- `ApiResponse` 统一返回 + `GlobalExceptionHandler`
- Swagger / Springdoc OpenAPI 接入
- `collector.mode: mock` 配置

交付物：Swagger 页面上所有核心 API 可调通。

提交示例：
```
feat: 实现 Mock 数据生成器覆盖七种活动类型
feat: 新增 CollectorController 提供启动/停止/状态接口
feat: 接入 Springdoc OpenAPI
```

---

### M3 —— 前端四页 + 轮询（tag v0.4.0）

**目标**：前端能看到完整 Mock 数据；**完成后立刻截图留存**。

任务（对应 PLAN 第 5.4 节）：
- Dashboard：采集状态、开始/停止、今日时长、活动数、类型占比图、最近活动
- Timeline：按日期查询、按类型筛选、显示时长与应用/窗口/描述/关键词/置信度
- Reports：查看/生成/重新生成日报
- Notes：输入内容、选时间、加标签
- 轮询：采集状态 5s、最近活动 10s（不引入 WebSocket）
- Axios 封装 + 类型定义

交付物：四页可交互 + **README 用截图/GIF**。

提交示例：
```
feat: 实现今日工作台页面与采集状态轮询
feat: 实现时间线页面与类型筛选
feat: 实现日报页面
docs: 补充前端界面截图
```

---

### M4 —— Windows 采集（tag v0.5.0）

**目标**：真实读取前台窗口 + 低频截图。**此里程碑起需要 Windows 环境。**

任务（对应 PLAN 第 5.1、8 节）：
- JNA 读取前台窗口：应用名、窗口标题、时间
- AWT Robot 截图（每 2 分钟最多一张，窗口变化时触发）
- 敏感进程黑名单过滤（截图**之前**判断）：WeChat.exe / Alipay.exe / 1Password.exe / KeePass.exe
- 截图使用 `try/finally` 确保删除
- 相邻同类型活动合并（单活动最多 30 分钟）
- CollectorService 状态机：start / stop / status / capture-now

交付物：Windows 下真实采集并落库 observation。

提交示例：
```
feat: 使用 JNA 读取前台窗口信息
feat: 实现 AWT Robot 低频截图与截图后删除
feat: 实现敏感应用黑名单过滤
```

---

### M5 —— 视觉模型接入（tag v0.6.0）

**目标**：把截图 + 窗口信息变成结构化活动事件。

任务（对应 PLAN 第 5.2 节）：
- `VisionActivityAnalyzer`：输入应用名/窗口标题/时间/临时截图
- 模型输出映射为结构化对象（type 固定枚举 + description + keywords + confidence）
- `ActivityType` 用 Java 枚举；`confidence` 限制 0~1
- 模型失败降级：用窗口信息生成事件，`source = WINDOW_FALLBACK`
- 模型 API Key 从**环境变量**读取

交付物：真实生成结构化 ActivityEvent；无 Key 时自动降级。

提交示例：
```
feat: 实现 VisionActivityAnalyzer 结构化活动分析
feat: 模型调用失败降级为窗口信息事件
```

---

### M6 —— Agent 日报 + 收尾（tag v1.0.0）★核心亮点

**目标**：日报功能真正跑通 + 项目收尾。**这是简历的重头戏，留足时间。**

任务（对应 PLAN 第 5.3、8、12 节）：
- LangChain4j Agent：`DailyReportAgent.chat("请生成今天的工作日报")`
- 三个受限工具：`queryActivityEvents(date)` / `queryUserNotes(date)` / `saveDailyReport(report)`
- 工具边界：只能查指定日期、不能访问任意文件、只能 upsert 日报
- `report_date` 唯一 + upsert 幂等
- Spring Scheduler：每天 22:00 触发
- 日报结构：date / timeline / achievements / learning / distractions / nextActions
- 隐私收尾：API 只监听 127.0.0.1，日志不输出敏感信息
- 构建脚本（前端 build 后复制到后端 `static`）
- **README 完善：效果图 + 架构图 + 核心亮点小节 + 快速开始**

交付物：v1.0.0 可演示的完整项目 + 成品 README。

提交示例：
```
feat: 实现 LangChain4j 日报 Agent 与三个受限工具
feat: 接入 Spring Scheduler 每日 22:00 生成日报
feat: 日报 upsert 保证重复生成幂等
docs: 完善 README 效果图与核心亮点
```

---

## 5. Git 与 GitHub 规范

### 分支策略（单人简化版）

- 日常直接在 `main` 上小步提交，**不搞** feature 分支 + 自 PR（单人副项目显得刻意）。
- 每个里程碑完成时打 tag 并发 Release。
- 若某里程碑想留独立记录，可临时开分支，完成后合回 `main`（可选）。

### 提交规范（Conventional Commits）

```text
feat:     新功能
fix:      修复 bug
docs:     文档
refactor:  重构
test:     测试
chore:     构建/配置/杂项
```

一次提交只做一件事，message 用中文或英文均可，保持统一。

### 版本与 Release

- 里程碑打 tag：`v0.1.0` → `v1.0.0`（见总览表）。
- 每次 tag 在 GitHub 发 Release，notes 写清本版新增内容。

### 必备规范文件

| 文件 | 作用 |
|---|---|
| `README.md` | 项目门面（见第 6 节结构） |
| `.gitignore` | 排除构建产物、依赖、临时截图、`.env` |
| `LICENSE` | MIT |
| `.github/workflows/ci.yml` | 每次 push 自动 build |

---

## 6. README 结构（门面）

按此顺序，**第一屏就要抓住人**：

1. **项目名 + 一句话定位**
2. **效果图 / GIF**（前端工作台 + 时间线 + 日报截图拼图）← 最重要
3. **技术栈徽章**：Java 17 / Spring Boot / LangChain4j / React / TypeScript / SQLite
4. **架构图**（复用 PLAN 第 4 节）
5. **核心亮点**（4–5 条，见下）
6. **快速开始**：环境要求、Mock 模式运行、Windows 模式运行
7. **API 一览**（复用 PLAN 第 7 节）
8. **Roadmap / 已完成**（对应里程碑）

核心亮点小节（直接列）：
- LangChain4j Agent Tool Calling 自动生成日报
- 视觉模型将屏幕内容转为结构化活动事件
- 低频采样 + 敏感应用过滤 + 截图即删的隐私设计
- 日报 upsert 幂等 + 模型失败降级
- React + ECharts 可视化时间线与统计

---

## 7. 简历表达

简历 bullet（可直接改）：

```
· 基于 Spring Boot + LangChain4j 的个人工作复盘系统：JNA 采集前台窗口、
  AWT Robot 低频截图，调用视觉模型将屏幕内容转化为结构化活动事件并落库 SQLite。
· 实现 LangChain4j Agent 工具调用（Tool Calling）自动生成每日日报：Agent 通过
  三个受限工具查询活动与笔记并保存，report_date 唯一约束 + upsert 保证幂等。
· 设计敏感应用黑名单过滤、分析后删除原始截图、模型失败降级等隐私与成本控制；
  前端 React + Ant Design + ECharts 可视化时间线、统计与日报。
```

结构：**链路 → 亮点(Agent) → 工程细节**。

面试话术见 `PROJECT_PLAN.md` 第 13 节。

---

## 8. 跨 session 协作方式

- **每次 session 开头**：读本计划 + `PROJECT_PLAN.md` + `git log`，对齐当前进度。
- **每次 session 结束**：commit 当前里程碑成果，把进度写进记忆。
- 用户可用一句话指路（如「继续做 M3 的日报页」），即可从对应位置接续。
- **push 的分工**：Git 本地操作（init / commit / tag）可在沙箱完成并真实落在 `D:\WhatADay`；**push 到 GitHub 需要用户凭证**（SSH key 或 token），由用户执行，或提供仅授权该仓库的 fine-grained token。

---

## 9. 风险与范围压缩策略

- **最大风险**：日报功能被拖到最后、来不及做。
  **对策**：M6 单独留足时间；Mock 先行保证任何时候都有可展示成果。
- **环境风险**：无 Windows 环境时，M4/M5 暂停，先做 Mock 与文档。
- **模型成本/Key 风险**：无 Key 时靠 Mock + 降级流程完整演示。
- **范围压缩顺序**（从先砍到后砍）：娱乐性细节 → 复杂跨平台 → CI 高级功能 → 前端精致度 → **日报链路（绝不砍）**。

---

## 10. 开工前需确认

1. **GitHub 署名**：用户名 + 邮箱（用于 git 提交身份）
2. **构建工具**：Maven 还是 Gradle？（建议 Gradle）
3. **GitHub 仓库**：名称、是否 public

确认后即可从 M0 开始。
