package com.example.whataday.report;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.note.NoteRepository;
import com.example.whataday.note.UserNote;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 日报 Agent 可以使用的全部工具。
 *
 * <p>这是 Agent 与数据之间的<b>唯一</b>通道，也是整个项目在安全边界上最要紧的一处设计。
 * 三个工具刻意做得很窄：
 * <ul>
 *   <li>{@code queryActivityEvents(date)} —— 只能按日期查活动，参数就是一个日期字符串，
 *       模型没有任何途径指定路径、SQL 或文件，因此不可能读到日期之外的东西</li>
 *   <li>{@code queryUserNotes(date)} —— 同理，只能查手动记录</li>
 *   <li>{@code saveDailyReport(...)} —— 只按日期 upsert 日报，不能改活动、不能改记录，
 *       也不能删除任何东西</li>
 * </ul>
 *
 * <p>另外两个刻意的取舍：
 * <ol>
 *   <li>查询类工具返回<b>格式化好的紧凑文本</b>而不是原始对象。一天可能有上百条活动，
 *       直接抛 JSON 既费 token 又难读；格式化后还能把类型统计一并给模型做判断依据。</li>
 *   <li>参数校验失败时返回一句说明而<b>不是抛异常</b>，这样模型可以自己改正日期格式重试，
 *       而不是让整次生成直接失败。</li>
 * </ol>
 */
@Component
public class ReportTools {

    private static final Logger log = LoggerFactory.getLogger(ReportTools.class);

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** 单次查询返回的最大活动条数，避免超长的一天把上下文撑爆。 */
    private static final int MAX_ACTIVITY_LINES = 200;

    private final ActivityRepository activityRepository;
    private final NoteRepository noteRepository;
    private final ReportRepository reportRepository;

    public ReportTools(ActivityRepository activityRepository,
                       NoteRepository noteRepository,
                       ReportRepository reportRepository) {
        this.activityRepository = activityRepository;
        this.noteRepository = noteRepository;
        this.reportRepository = reportRepository;
    }

    @Tool("查询指定日期采集到的活动事件，返回当天的类型统计与按时间排序的活动列表。")
    public String queryActivityEvents(@P("日期，格式必须是 yyyy-MM-dd，例如 2026-09-13") String date) {
        LocalDate day = parseDate(date);
        if (day == null) {
            return "日期格式不正确，请使用 yyyy-MM-dd 格式重新调用，例如 2026-09-13。收到的是：" + date;
        }

        List<ActivityEvent> events = activityRepository.findByDate(day);
        if (events.isEmpty()) {
            return day + " 没有采集到任何活动。";
        }

        long activeMinutes = events.stream()
                .filter(e -> e.startTime() != null && e.endTime() != null)
                .mapToLong(e -> Math.max(0, Duration.between(e.startTime(), e.endTime()).toMinutes()))
                .sum();

        StringBuilder builder = new StringBuilder();
        builder.append(day).append(" 共 ").append(events.size()).append(" 条活动，累计活跃 ")
                .append(activeMinutes).append(" 分钟。\n");
        builder.append("类型分布：").append(typeBreakdown(events)).append("\n\n");
        builder.append("时间线（按时间排序）：\n");

        int limit = Math.min(events.size(), MAX_ACTIVITY_LINES);
        for (int i = 0; i < limit; i++) {
            ActivityEvent event = events.get(i);
            builder.append("- ")
                    .append(range(event))
                    .append(" [").append(event.type().label()).append("] ")
                    .append(describe(event));
            if (event.keywords().size() > 0) {
                builder.append("（关键词：").append(String.join("、", event.keywords())).append("）");
            }
            builder.append('\n');
        }
        if (events.size() > limit) {
            builder.append("…… 其余 ").append(events.size() - limit).append(" 条已省略。\n");
        }
        return builder.toString();
    }

    @Tool("查询指定日期用户手动记录的内容，返回按时间排序的记录列表。")
    public String queryUserNotes(@P("日期，格式必须是 yyyy-MM-dd，例如 2026-09-13") String date) {
        LocalDate day = parseDate(date);
        if (day == null) {
            return "日期格式不正确，请使用 yyyy-MM-dd 格式重新调用，例如 2026-09-13。收到的是：" + date;
        }

        List<UserNote> notes = noteRepository.findByDate(day);
        if (notes.isEmpty()) {
            return day + " 没有手动记录。";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(day).append(" 共 ").append(notes.size()).append(" 条手动记录：\n");
        for (UserNote note : notes) {
            builder.append("- ");
            if (note.noteTime() != null) {
                builder.append(HH_MM.format(note.noteTime())).append(' ');
            }
            builder.append(note.content());
            if (!note.tags().isEmpty()) {
                builder.append(" [").append(String.join("、", note.tags())).append("]");
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    @Tool("""
            保存指定日期的工作日报。必须在查询活动和手动记录之后调用，每个字段都要填；
            确实没有内容的字段传空列表，不要省略字段，也不要编造内容。
            """)
    public String saveDailyReport(
            @P("日报日期，格式必须是 yyyy-MM-dd") String date,
            @P("当天时间线，按时间顺序的简短条目") List<String> timeline,
            @P("当天完成的具体成果，没有则传空列表") List<String> achievements,
            @P("当天学到的东西，没有则传空列表") List<String> learning,
            @P("分心或低效的事项，没有则传空列表") List<String> distractions,
            @P("下一步行动") List<String> nextActions) {

        LocalDate day = parseDate(date);
        if (day == null) {
            return "保存失败：日期格式不正确，请使用 yyyy-MM-dd 格式重新调用。收到的是：" + date;
        }

        if (isEmpty(timeline) && isEmpty(achievements) && isEmpty(learning)
                && isEmpty(distractions) && isEmpty(nextActions)) {
            return "保存失败：所有字段都为空，没有可保存的内容。请先查询当天的活动与记录。";
        }

        // upsert：同一天重复保存只会更新同一条记录，不会产生重复日报
        reportRepository.upsert(new DailyReport(
                day, timeline, achievements, learning, distractions, nextActions, null, null));

        log.info("日报 Agent 已保存 {} 的日报", day);
        return "已保存 " + day + " 的日报。";
    }

    private static String typeBreakdown(List<ActivityEvent> events) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ActivityEvent event : events) {
            counts.merge(event.type().label(), 1L, Long::sum);
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((label, count) -> parts.add(label + " " + count));
        return String.join("、", parts);
    }

    private static String range(ActivityEvent event) {
        String start = event.startTime() == null ? "--:--" : HH_MM.format(event.startTime());
        String end = event.endTime() == null ? "进行中" : HH_MM.format(event.endTime());
        return start + "-" + end;
    }

    private static String describe(ActivityEvent event) {
        if (event.description() != null && !event.description().isBlank()) {
            return event.description();
        }
        if (event.windowTitle() != null && !event.windowTitle().isBlank()) {
            return event.windowTitle();
        }
        return event.appName() == null ? "未识别活动" : event.appName();
    }

    private static boolean isEmpty(List<String> values) {
        return values == null || values.stream().allMatch(value -> value == null || value.isBlank());
    }

    /** 解析日期；格式不对返回 {@code null}，由调用方回一句可读的提示让模型改正。 */
    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
