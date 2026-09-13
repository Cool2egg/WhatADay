package com.example.whataday.activity;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 活动事件的查询与统计。 */
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /** 今日活动。 */
    public List<ActivityEvent> today() {
        return activityRepository.findByDate(LocalDate.now());
    }

    /**
     * 按日期与类型查询时间线。
     *
     * @param date 查询日期，{@code null} 视为今天
     * @param type 活动类型筛选，{@code null} 表示不筛选
     */
    public List<ActivityEvent> timeline(LocalDate date, ActivityType type) {
        LocalDate target = date != null ? date : LocalDate.now();
        return type == null
                ? activityRepository.findByDate(target)
                : activityRepository.findByDateAndType(target, type);
    }

    /** 汇总某一天的统计信息。{@code countByType} 会补齐全部类型，数量为 0 的也保留。 */
    public ActivitySummary summary(LocalDate date, ActivityType type) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<ActivityEvent> events = timeline(target, type);

        long activeMinutes = events.stream()
                .filter(e -> e.startTime() != null && e.endTime() != null)
                .mapToLong(e -> Math.max(0, Duration.between(e.startTime(), e.endTime()).toMinutes()))
                .sum();

        Map<ActivityType, Long> countByType = new EnumMap<>(ActivityType.class);
        for (ActivityType each : ActivityType.values()) {
            countByType.put(each, 0L);
        }
        events.forEach(e -> countByType.merge(e.type(), 1L, Long::sum));

        return new ActivitySummary(target, events.size(), activeMinutes, countByType);
    }
}
