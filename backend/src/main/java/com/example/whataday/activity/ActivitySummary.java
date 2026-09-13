package com.example.whataday.activity;

import java.time.LocalDate;
import java.util.Map;

/**
 * 某一天的活动汇总（前端「今日工作台」的统计卡片与类型占比图）。
 *
 * @param date          统计日期
 * @param eventCount    活动数量
 * @param activeMinutes 活动累计时长（分钟），仅统计有结束时间的事件
 * @param countByType   各活动类型的数量，未出现的类型也会以 0 出现，方便前端直接画图
 */
public record ActivitySummary(
        LocalDate date,
        int eventCount,
        long activeMinutes,
        Map<ActivityType, Long> countByType
) {
}
