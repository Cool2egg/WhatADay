package com.example.whataday.activity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 结构化活动事件。
 *
 * @param id            主键，插入前为 {@code null}
 * @param observationId 关联的采集观察 id，可为 {@code null}
 * @param startTime     开始时间
 * @param endTime       结束时间，进行中的活动可为 {@code null}
 * @param appName       应用名称
 * @param windowTitle   窗口标题
 * @param type          活动类型
 * @param description   活动描述
 * @param keywords      关键词，不会为 {@code null}
 * @param confidence    置信度，恒在 {@code [0, 1]}
 * @param source        来源（视觉模型 / 窗口降级）
 * @param createdAt     入库时间
 */
public record ActivityEvent(
        Long id,
        Long observationId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String appName,
        String windowTitle,
        ActivityType type,
        String description,
        List<String> keywords,
        Double confidence,
        ActivitySource source,
        LocalDateTime createdAt
) {

    /** 归一化：关键词兜底为空列表，置信度夹紧到 [0, 1]。 */
    public ActivityEvent {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        if (confidence != null) {
            confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
        if (type == null) {
            type = ActivityType.OTHER;
        }
        if (source == null) {
            source = ActivitySource.VISION;
        }
    }
}
