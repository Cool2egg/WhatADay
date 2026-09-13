package com.example.whataday.report;

import com.example.whataday.RepositoryTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.note.NoteRepository;
import com.example.whataday.note.UserNote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent 工具测试。
 *
 * <p>这些工具是 Agent 与数据之间的唯一通道，所以除了「功能对不对」，
 * 更要验证「边界严不严」：日期格式不合法时给可读提示而不是抛异常（模型能自己改正），
 * 保存走 upsert 保证幂等，空内容不落库。
 */
class ReportToolsTest extends RepositoryTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 13);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ReportRepository reportRepository;

    private ReportTools tools;

    @BeforeEach
    void setUpTools() {
        tools = new ReportTools(activityRepository, noteRepository, reportRepository);
    }

    @Test
    void queryActivityEventsReturnsBreakdownAndTimeline() {
        insertActivity(LocalTime.of(9, 0), LocalTime.of(11, 30), ActivityType.CODING,
                "修改优惠券领取逻辑", List.of("Java", "优惠券"));
        insertActivity(LocalTime.of(14, 0), LocalTime.of(15, 0), ActivityType.MEETING,
                "组会同步进度", List.of("组会"));

        String result = tools.queryActivityEvents("2026-09-13");

        assertThat(result).contains("2026-09-13");
        assertThat(result).contains("共 2 条活动");
        assertThat(result).contains("累计活跃 210 分钟");
        assertThat(result).contains("编码 1");
        assertThat(result).contains("会议 1");
        assertThat(result).contains("09:00-11:30 [编码] 修改优惠券领取逻辑");
        assertThat(result).contains("关键词：Java、优惠券");
    }

    @Test
    void queryActivityEventsOnEmptyDayExplainsInsteadOfFailing() {
        assertThat(tools.queryActivityEvents("2026-09-13")).contains("没有采集到任何活动");
        assertThat(tools.queryUserNotes("2026-09-13")).contains("没有手动记录");
    }

    @Test
    void invalidDateReturnsCorrectableMessageInsteadOfThrowing() {
        // 不抛异常是关键：模型看到提示后可以自己改成正确格式重试
        assertThat(tools.queryActivityEvents("2026/09/13")).contains("日期格式不正确");
        assertThat(tools.queryActivityEvents(null)).contains("日期格式不正确");
        assertThat(tools.queryUserNotes("今天是几号")).contains("日期格式不正确");
    }

    @Test
    void queryUserNotesFormatsTimeContentAndTags() {
        noteRepository.insert(new UserNote(null, LocalDateTime.of(DAY, LocalTime.of(12, 5)),
                "上午把领取逻辑改完了", List.of("完成"), null));

        String result = tools.queryUserNotes("2026-09-13");

        assertThat(result).contains("共 1 条手动记录");
        assertThat(result).contains("12:05 上午把领取逻辑改完了 [完成]");
    }

    @Test
    void saveDailyReportPersistsAllSections() {
        String result = tools.saveDailyReport("2026-09-13",
                List.of("09:00-11:30 写优惠券逻辑"),
                List.of("完成领取逻辑"),
                List.of("LangChain4j Tool Calling"),
                List.of("刷了会视频"),
                List.of("写单元测试"));

        assertThat(result).contains("已保存");

        DailyReport saved = reportRepository.findByDate(DAY).orElseThrow();
        assertThat(saved.timeline()).containsExactly("09:00-11:30 写优惠券逻辑");
        assertThat(saved.achievements()).containsExactly("完成领取逻辑");
        assertThat(saved.learning()).containsExactly("LangChain4j Tool Calling");
        assertThat(saved.distractions()).containsExactly("刷了会视频");
        assertThat(saved.nextActions()).containsExactly("写单元测试");
    }

    @Test
    void savingTwiceUpdatesTheSameRecord() {
        tools.saveDailyReport("2026-09-13", List.of("第一版"), List.of(), List.of(), List.of(), List.of());
        tools.saveDailyReport("2026-09-13", List.of("第二版"), List.of("加了成果"), List.of(), List.of(), List.of());

        // Agent 有可能重复调用保存工具，这里保证不会产生两条日报
        assertThat(reportRepository.count()).isEqualTo(1);
        DailyReport saved = reportRepository.findByDate(DAY).orElseThrow();
        assertThat(saved.timeline()).containsExactly("第二版");
        assertThat(saved.achievements()).containsExactly("加了成果");
    }

    @Test
    void savingEmptyReportIsRejected() {
        String result = tools.saveDailyReport("2026-09-13",
                List.of(), List.of(), List.of(), List.of(), List.of("   "));

        assertThat(result).contains("保存失败");
        assertThat(reportRepository.count()).isZero();
    }

    @Test
    void savingWithInvalidDateIsRejected() {
        String result = tools.saveDailyReport("13-09-2026",
                List.of("内容"), List.of(), List.of(), List.of(), List.of());

        assertThat(result).contains("保存失败");
        assertThat(reportRepository.count()).isZero();
    }

    private void insertActivity(LocalTime start, LocalTime end, ActivityType type,
                                String description, List<String> keywords) {
        activityRepository.insert(new ActivityEvent(null, null,
                LocalDateTime.of(DAY, start), LocalDateTime.of(DAY, end),
                "IntelliJ IDEA", "CouponService.java", type, description,
                keywords, 0.9, ActivitySource.VISION, null));
    }
}
