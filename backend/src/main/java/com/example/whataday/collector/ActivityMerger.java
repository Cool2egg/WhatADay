package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * 相邻活动合并。
 *
 * <p>如果每 30 秒采集一次就写一条活动，一天下来会是几千条零时长碎片，时间线完全没法看。
 * 合并规则来自方案第 5.1 节：<b>应用、窗口标题、活动类型三者都相同</b>的相邻事件可以合并，
 * 且单个活动最多合并到 {@code maxMergeMinutes}（默认 30 分钟）。
 *
 * <p>顺带一提，这里也是「单活动不超过 30 分钟」这条约束的落点——
 * 它防止用户挂着同一个窗口一整天，最后生成一条 10 小时的「活动」。
 */
@Component
public class ActivityMerger {

    private final CollectorProperties properties;

    public ActivityMerger(CollectorProperties properties) {
        this.properties = properties;
    }

    /** 新事件能否并入已有事件。 */
    public boolean canMerge(ActivityEvent existing, ActivityEvent incoming) {
        if (!sameText(existing.appName(), incoming.appName())) {
            return false;
        }
        if (!sameText(existing.windowTitle(), incoming.windowTitle())) {
            return false;
        }
        if (!Objects.equals(existing.type(), incoming.type())) {
            return false;
        }
        if (existing.startTime() == null || incoming.startTime() == null) {
            return false;
        }

        LocalDateTime existingEnd = existing.endTime() != null ? existing.endTime() : existing.startTime();

        // 中间断开太久（例如离开了很久又回来）就不算同一段连续工作
        long gapMinutes = Duration.between(existingEnd, incoming.startTime()).toMinutes();
        if (gapMinutes > properties.getMergeGapMinutes()) {
            return false;
        }

        // 合并后的总时长不能超过单个活动的上限
        long mergedMinutes = Duration.between(existing.startTime(), incoming.startTime()).toMinutes();
        return mergedMinutes <= properties.getMaxMergeMinutes();
    }

    /** 合并：保留原有事件的身份与内容，只把结束时间推进到新事件。 */
    public ActivityEvent merge(ActivityEvent existing, ActivityEvent incoming) {
        LocalDateTime end = incoming.endTime() != null ? incoming.endTime() : incoming.startTime();
        return new ActivityEvent(
                existing.id(),
                existing.observationId(),
                existing.startTime(),
                end,
                existing.appName(),
                existing.windowTitle(),
                existing.type(),
                existing.description(),
                existing.keywords(),
                existing.confidence(),
                existing.source(),
                existing.createdAt());
    }

    private static boolean sameText(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
