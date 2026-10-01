package com.example.whataday.activity;

import java.time.LocalDate;
import java.util.Map;

/**
 * 一段日期范围内的工作指标。
 *
 * <p>这些指标只描述数据库中已经记录的活动事实，不包含模型推断，
 * 方便工作助手在回答效率问题时把统计结果和语义记忆分开。</p>
 */
public record WorkMetrics(
        LocalDate startDate,
        LocalDate endDate,
        int eventCount,
        long activeMinutes,
        long focusedMinutes,
        long distractionMinutes,
        int typeSwitchCount,
        Map<ActivityType, Long> minutesByType
) {
}
