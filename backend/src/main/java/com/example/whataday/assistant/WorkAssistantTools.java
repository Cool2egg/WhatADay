package com.example.whataday.assistant;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivityService;
import com.example.whataday.activity.WorkMetrics;
import com.example.whataday.memory.WorkMemorySearchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 工作助手唯一可用的数据工具。 */
@Component
public class WorkAssistantTools {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final WorkMemorySearchService memorySearchService;

    public WorkAssistantTools(ActivityService activityService,
                              ActivityRepository activityRepository,
                              WorkMemorySearchService memorySearchService) {
        this.activityService = activityService;
        this.activityRepository = activityRepository;
        this.memorySearchService = memorySearchService;
    }

    @Tool("查询日期范围内的工作时长、专注时长、分心时长和活动类型切换次数")
    public String queryWorkMetrics(
            @P("开始日期，格式 yyyy-MM-dd") String startDate,
            @P("结束日期，格式 yyyy-MM-dd") String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        if (start == null || end == null) {
            return "日期格式不正确，请使用 yyyy-MM-dd。";
        }
        try {
            WorkMetrics metrics = activityService.metrics(start, end);
            return "日期范围：" + metrics.startDate() + " 至 " + metrics.endDate()
                    + "\n活动数：" + metrics.eventCount()
                    + "\n累计活动时长：" + metrics.activeMinutes() + " 分钟"
                    + "\n专注时长（编码+学习）：" + metrics.focusedMinutes() + " 分钟"
                    + "\n分心时长（娱乐+浏览）：" + metrics.distractionMinutes() + " 分钟"
                    + "\n活动类型切换：" + metrics.typeSwitchCount() + " 次"
                    + "\n各类型时长：" + metrics.minutesByType();
        } catch (IllegalArgumentException e) {
            return "查询失败：" + e.getMessage();
        }
    }

    @Tool("查询某一天的结构化活动时间线，用于核对具体做了什么")
    public String queryActivityEvents(@P("日期，格式 yyyy-MM-dd") String date) {
        LocalDate day = parseDate(date);
        if (day == null) return "日期格式不正确，请使用 yyyy-MM-dd。";
        List<ActivityEvent> events = activityRepository.findByDate(day);
        if (events.isEmpty()) return day + " 没有活动记录。";
        StringBuilder result = new StringBuilder(day + " 的活动：\n");
        for (ActivityEvent event : events) {
            String start = event.startTime() == null ? "--:--" : HH_MM.format(event.startTime());
            String end = event.endTime() == null ? "进行中" : HH_MM.format(event.endTime());
            result.append("- ").append(start).append('-').append(end)
                    .append(" [").append(event.type().label()).append("] ")
                    .append(firstNonBlank(event.description(), event.windowTitle(), event.appName()))
                    .append('\n');
        }
        return result.toString();
    }

    @Tool("检索历史日报、手动记录和工作记忆，回答长期工作状态与任务连续性问题")
    public String searchWorkMemory(
            @P("要检索的工作主题或问题") String query,
            @P("最多返回多少条，建议 3 到 8") int topK) {
        List<WorkMemorySearchService.MemoryHit> hits = memorySearchService.search(query, topK);
        if (hits.isEmpty()) return "没有检索到相关工作记忆。";
        StringBuilder result = new StringBuilder("相关工作记忆：\n");
        for (WorkMemorySearchService.MemoryHit hit : hits) {
            result.append("- 来源：").append(hit.memory().sourceType())
                    .append("，日期：").append(hit.memory().memoryDate())
                    .append("，相关度：").append(String.format("%.3f", hit.score()))
                    .append("\n").append(hit.memory().content()).append("\n");
        }
        return result.toString();
    }

    private static LocalDate parseDate(String value) {
        try {
            return value == null ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "未识别活动";
    }
}
