package com.example.whataday.report;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.common.NotFoundException;
import com.example.whataday.note.NoteRepository;
import com.example.whataday.note.UserNote;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 日报的生成与查询。 */
@Service
public class ReportService {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ActivityRepository activityRepository;
    private final NoteRepository noteRepository;
    private final ReportRepository reportRepository;

    public ReportService(ActivityRepository activityRepository,
                         NoteRepository noteRepository,
                         ReportRepository reportRepository) {
        this.activityRepository = activityRepository;
        this.noteRepository = noteRepository;
        this.reportRepository = reportRepository;
    }

    public Optional<DailyReport> find(LocalDate date) {
        return reportRepository.findByDate(date);
    }

    /** 全部日报，按日期倒序。 */
    public List<DailyReport> list() {
        return reportRepository.findAll();
    }

    /**
     * 生成（或重新生成）某一天的日报。
     *
     * <p>重复调用是幂等的：日期是主键，写入走 upsert，同一天永远只有一条记录。
     * 当天既没有活动也没有手动记录时抛 404。
     *
     * <p><b>说明：</b>下面用「按活动类型挑选内容」的启发式规则生成日报，是 M2 的占位实现，
     * 目的是让前端与接口先行跑通。M6 会由 LangChain4j Agent 通过 Tool Calling 读取同一批数据，
     * 用模型生成真正有概括力的日报，届时替换本方法内部逻辑即可，接口与数据结构不变。
     */
    public DailyReport generate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();

        List<ActivityEvent> activities = activityRepository.findByDate(target);
        List<UserNote> notes = noteRepository.findByDate(target);

        if (activities.isEmpty() && notes.isEmpty()) {
            throw new NotFoundException(target + " 没有任何活动记录或手动记录，无法生成日报");
        }

        DailyReport report = new DailyReport(
                target,
                buildTimeline(activities, notes),
                descriptionsOf(activities, ActivityType.CODING),
                learningOf(activities),
                descriptionsOf(activities, ActivityType.ENTERTAINMENT),
                nextActionsOf(notes),
                null,
                null);

        reportRepository.upsert(report);
        // 读回落库结果，返回的 createdAt / updatedAt 才是真实值
        return reportRepository.findByDate(target)
                .orElseThrow(() -> new NotFoundException("日报写入后未能读回：" + target));
    }

    private static List<String> buildTimeline(List<ActivityEvent> activities, List<UserNote> notes) {
        List<TimelineEntry> entries = new ArrayList<>();
        activities.stream()
                .filter(a -> a.startTime() != null)
                .forEach(a -> entries.add(new TimelineEntry(a.startTime(), activityLine(a))));
        notes.stream()
                .filter(n -> n.noteTime() != null)
                .forEach(n -> entries.add(new TimelineEntry(n.noteTime(), "📝 " + n.content())));
        entries.sort(Comparator.comparing(TimelineEntry::at));
        return entries.stream().map(TimelineEntry::text).toList();
    }

    private static String activityLine(ActivityEvent event) {
        String start = HH_MM.format(event.startTime());
        String end = event.endTime() == null ? "进行中" : HH_MM.format(event.endTime());
        String what = firstNonBlank(event.description(), event.windowTitle(), event.appName());
        return "%s-%s %s".formatted(start, end, what);
    }

    private static List<String> descriptionsOf(List<ActivityEvent> activities, ActivityType type) {
        return activities.stream()
                .filter(a -> a.type() == type)
                .map(ActivityEvent::description)
                .filter(ReportService::notBlank)
                .distinct()
                .toList();
    }

    /** 学习内容：LEARNING 活动的描述，外加它们的关键词。 */
    private static List<String> learningOf(List<ActivityEvent> activities) {
        List<String> learning = new ArrayList<>(descriptionsOf(activities, ActivityType.LEARNING));
        activities.stream()
                .filter(a -> a.type() == ActivityType.LEARNING)
                .flatMap(a -> a.keywords().stream())
                .filter(ReportService::notBlank)
                .filter(keyword -> !learning.contains(keyword))
                .forEach(learning::add);
        return learning;
    }

    /** 下一步行动：用户手动记录中打了「下一步」标签的内容。 */
    private static List<String> nextActionsOf(List<UserNote> notes) {
        return notes.stream()
                .filter(n -> n.tags().contains("下一步"))
                .map(UserNote::content)
                .filter(ReportService::notBlank)
                .toList();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (notBlank(candidate)) {
                return candidate;
            }
        }
        return "未识别活动";
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private record TimelineEntry(LocalDateTime at, String text) {
    }
}
