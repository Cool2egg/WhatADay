package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.note.NoteRepository;
import com.example.whataday.note.UserNote;
import com.example.whataday.report.ReportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Mock 模式的示例数据。
 *
 * <p>这是正式功能的一部分，不是测试代码：没有 Windows 环境或模型 API Key 时，
 * 靠它就能把「时间线 → 统计 → 日报」整条链路演示完整。
 *
 * <p>两件事：
 * <ol>
 *   <li>应用启动时，如果当天还没有数据，就灌入一整天覆盖<b>全部七种活动类型</b>的示例活动与手动记录，
 *       并生成一份日报——保证前端一打开就有内容，面试演示时这一点很关键。</li>
 *   <li>{@link #appendCurrentActivity()} 供「立即采集」接口模拟一次新的采集。</li>
 * </ol>
 * 所有写入都是幂等的：当天已有数据时启动灌入不做任何事。
 */
@Component
@ConditionalOnProperty(name = "whataday.collector.mode", havingValue = "mock", matchIfMissing = true)
public class MockDataSeeder implements ApplicationRunner {

    private final ActivityRepository activityRepository;
    private final ObservationRepository observationRepository;
    private final NoteRepository noteRepository;
    private final ReportService reportService;

    public MockDataSeeder(ActivityRepository activityRepository,
                          ObservationRepository observationRepository,
                          NoteRepository noteRepository,
                          ReportService reportService) {
        this.activityRepository = activityRepository;
        this.observationRepository = observationRepository;
        this.noteRepository = noteRepository;
        this.reportService = reportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedIfEmpty(LocalDate.now());
    }

    /** 幂等：当天已有活动或手动记录时直接返回。 */
    public synchronized void seedIfEmpty(LocalDate date) {
        boolean hasData = !activityRepository.findByDate(date).isEmpty()
                || !noteRepository.findByDate(date).isEmpty();
        if (hasData) {
            return;
        }
        seedDay(date);
        reportService.generate(date);
    }

    /** 模拟一次「立即采集」：写入一条进行中的活动。 */
    public synchronized int appendCurrentActivity() {
        LocalDateTime now = LocalDateTime.now();
        addEvent(now, null, "IntelliJ IDEA", "CouponService.java",
                ActivityType.CODING, "立即采集：继续修改优惠券逻辑",
                List.of("Java", "优惠券"), 0.9);
        return 1;
    }

    private void seedDay(LocalDate date) {
        // 覆盖面尽量广，让时间线、类型占比图和日报都有内容可展示
        addEvent(LocalDateTime.of(date, LocalTime.of(9, 0)), LocalDateTime.of(date, LocalTime.of(11, 30)),
                "IntelliJ IDEA", "CouponService.java",
                ActivityType.CODING, "修改优惠券领取逻辑",
                List.of("Java", "Spring Boot", "优惠券"), 0.93);

        addEvent(LocalDateTime.of(date, LocalTime.of(11, 30)), LocalDateTime.of(date, LocalTime.of(12, 0)),
                "Chrome", "掘金 - 优惠券系统设计",
                ActivityType.BROWSING, "浏览技术文章",
                List.of("掘金", "系统设计"), 0.81);

        addEvent(LocalDateTime.of(date, LocalTime.of(12, 30)), LocalDateTime.of(date, LocalTime.of(13, 0)),
                "Excel", "报销单.xlsx",
                ActivityType.OTHER, "整理报销表格",
                List.of("Excel"), 0.62);

        addEvent(LocalDateTime.of(date, LocalTime.of(14, 0)), LocalDateTime.of(date, LocalTime.of(15, 0)),
                "腾讯会议", "组会 - 进度同步",
                ActivityType.MEETING, "组会同步项目进度",
                List.of("组会", "进度同步"), 0.95);

        addEvent(LocalDateTime.of(date, LocalTime.of(15, 0)), LocalDateTime.of(date, LocalTime.of(17, 0)),
                "IntelliJ IDEA", "CacheService.java",
                ActivityType.CODING, "接入 Redis 缓存",
                List.of("Redis", "缓存"), 0.90);

        addEvent(LocalDateTime.of(date, LocalTime.of(17, 0)), LocalDateTime.of(date, LocalTime.of(18, 0)),
                "Bilibili", "LangChain4j 教程",
                ActivityType.LEARNING, "学习 LangChain4j Tool Calling",
                List.of("LangChain4j", "Tool Calling"), 0.88);

        addEvent(LocalDateTime.of(date, LocalTime.of(19, 0)), LocalDateTime.of(date, LocalTime.of(19, 30)),
                "微信", "毕设讨论",
                ActivityType.COMMUNICATION, "和同学讨论毕设方案",
                List.of("毕设"), 0.79);

        addEvent(LocalDateTime.of(date, LocalTime.of(21, 0)), LocalDateTime.of(date, LocalTime.of(21, 45)),
                "Steam", "Steam 商店",
                ActivityType.ENTERTAINMENT, "玩了一会游戏",
                List.of("Steam"), 0.86);

        noteRepository.insert(new UserNote(null, LocalDateTime.of(date, LocalTime.of(12, 5)),
                "上午把优惠券领取逻辑改完了", List.of("完成"), null));
        noteRepository.insert(new UserNote(null, LocalDateTime.of(date, LocalTime.of(18, 10)),
                "今天学了 LangChain4j 的 Tool Calling", List.of("学习"), null));
        noteRepository.insert(new UserNote(null, LocalDateTime.of(date, LocalTime.of(21, 50)),
                "明天要写日报模板", List.of("下一步"), null));
    }

    /**
     * 成对写入「观察记录 + 活动事件」，还原真实的「采集 → 分析」结果，
     * 而不是直接往活动表塞数据。
     */
    private void addEvent(LocalDateTime start, LocalDateTime end, String appName, String windowTitle,
                          ActivityType type, String description, List<String> keywords, double confidence) {
        long observationId = observationRepository.insert(new CaptureObservation(
                null, start, appName, windowTitle, AnalysisStatus.ANALYZED, null, null));

        activityRepository.insert(new ActivityEvent(
                null, observationId, start, end, appName, windowTitle,
                type, description, keywords, confidence, ActivitySource.VISION, null));
    }
}
