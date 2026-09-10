package com.example.whataday.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日工作日报。
 *
 * <p>{@code reportDate} 在库中是主键，天然唯一；重复生成走 upsert，
 * 因此同一天永远只有一条日报，{@code createdAt} 保留首次生成时间，
 * {@code updatedAt} 记录最后一次重新生成的时间。
 *
 * @param reportDate   日报日期
 * @param timeline     当天时间线
 * @param achievements 成果
 * @param learning     学习
 * @param distractions 分心事项
 * @param nextActions  下一步行动
 * @param createdAt    首次生成时间
 * @param updatedAt    最后更新时间
 */
public record DailyReport(
        LocalDate reportDate,
        List<String> timeline,
        List<String> achievements,
        List<String> learning,
        List<String> distractions,
        List<String> nextActions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 归一化：所有列表兜底为空列表，避免 {@code null} 污染 JSON 列。 */
    public DailyReport {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        achievements = achievements == null ? List.of() : List.copyOf(achievements);
        learning = learning == null ? List.of() : List.copyOf(learning);
        distractions = distractions == null ? List.of() : List.copyOf(distractions);
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
    }
}
