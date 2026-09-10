-- =====================================================================
-- WhatADay 数据库表结构（SQLite）
--
-- 约定：
--   * 时间字段统一存 ISO-8601 文本（如 2026-09-10T21:30:00），可直接按字符串范围比较
--   * JSON 列以 _json 结尾，存数组/对象的 JSON 文本
--   * 全部 CREATE ... IF NOT EXISTS，保证每次启动幂等
--   * 第一版使用 JdbcTemplate + schema.sql，不使用 JPA
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. capture_observation：采集器观察到的事实（未经过模型理解）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS capture_observation (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    observed_at     TEXT    NOT NULL,
    app_name        TEXT,
    window_title    TEXT,
    -- PENDING / ANALYZED / FAILED / IGNORED
    analysis_status TEXT    NOT NULL DEFAULT 'PENDING',
    analysis_error  TEXT,
    created_at      TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_observation_observed_at
    ON capture_observation (observed_at);

-- ---------------------------------------------------------------------
-- 2. activity_event：结构化活动事件（视觉模型或降级逻辑生成）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS activity_event (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    observation_id  INTEGER,
    start_time      TEXT    NOT NULL,
    end_time        TEXT,
    app_name        TEXT,
    window_title    TEXT,
    -- CODING / LEARNING / MEETING / BROWSING / ENTERTAINMENT / COMMUNICATION / OTHER
    type            TEXT    NOT NULL,
    description     TEXT,
    keywords_json   TEXT,
    confidence      REAL,
    -- VISION / WINDOW_FALLBACK
    source          TEXT    NOT NULL DEFAULT 'VISION',
    created_at      TEXT    NOT NULL,
    FOREIGN KEY (observation_id) REFERENCES capture_observation (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_activity_start_time
    ON activity_event (start_time);
CREATE INDEX IF NOT EXISTS idx_activity_type
    ON activity_event (type);
CREATE INDEX IF NOT EXISTS idx_activity_observation
    ON activity_event (observation_id);

-- ---------------------------------------------------------------------
-- 3. user_note：用户手动记录，参与日报生成
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_note (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    note_time   TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    tags_json   TEXT,
    created_at  TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_note_time
    ON user_note (note_time);

-- ---------------------------------------------------------------------
-- 4. daily_report：每日日报
--
-- report_date 作主键，天然唯一；配合 Repository 的
-- INSERT ... ON CONFLICT(report_date) DO UPDATE 实现 upsert，
-- 保证「重复生成日报」幂等。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_report (
    report_date       TEXT    PRIMARY KEY,
    timeline_json     TEXT,
    achievements_json TEXT,
    learning_json     TEXT,
    distractions_json TEXT,
    next_actions_json TEXT,
    created_at        TEXT    NOT NULL,
    updated_at        TEXT    NOT NULL
);
