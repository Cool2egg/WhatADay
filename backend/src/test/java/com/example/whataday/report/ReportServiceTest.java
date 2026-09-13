package com.example.whataday.report;

import com.example.whataday.RepositoryTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.common.NotFoundException;
import com.example.whataday.note.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 日报生成的两条路径。
 *
 * <p>重点验证「降级」：没有模型、Agent 抛异常、Agent 跑完却没落库——
 * 这三种情况下日报功能都必须照样可用，因为「无 Key 也能完整演示」是这个项目的底线。
 */
class ReportServiceTest extends RepositoryTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 13);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private ReportRepository reportRepository;

    private ReportTools tools;

    @BeforeEach
    void setUp() {
        tools = new ReportTools(activityRepository, noteRepository, reportRepository);
        activityRepository.insert(new ActivityEvent(null, null,
                LocalDateTime.of(DAY, LocalTime.of(9, 0)), LocalDateTime.of(DAY, LocalTime.of(11, 30)),
                "IntelliJ IDEA", "CouponService.java", ActivityType.CODING, "修改优惠券领取逻辑",
                List.of("Java"), 0.9, ActivitySource.VISION, null));
    }

    private ReportService serviceWith(DailyReportAgent agent) {
        return new ReportService(activityRepository, noteRepository, reportRepository,
                Optional.ofNullable(agent));
    }

    @Test
    void withoutAgentFallsBackToHeuristic() {
        DailyReport report = serviceWith(null).generate(DAY);

        // 启发式实现按活动类型挑选内容
        assertThat(report.achievements()).containsExactly("修改优惠券领取逻辑");
        assertThat(report.timeline()).hasSize(1);
    }

    @Test
    void usesAgentOutputWhenAgentSavesReport() {
        DailyReportAgent agent = message -> {
            tools.saveDailyReport(DAY.toString(),
                    List.of("上午集中写了优惠券领取逻辑"),
                    List.of("完成领取逻辑并自测通过"),
                    List.of("复习了 Spring 事务传播"),
                    List.of(),
                    List.of("补充边界用例的单元测试"));
            return "日报已生成。";
        };

        DailyReport report = serviceWith(agent).generate(DAY);

        // 内容来自 Agent，而不是启发式规则
        assertThat(report.timeline()).containsExactly("上午集中写了优惠券领取逻辑");
        assertThat(report.learning()).containsExactly("复习了 Spring 事务传播");
        assertThat(report.nextActions()).containsExactly("补充边界用例的单元测试");
    }

    @Test
    void fallsBackWhenAgentThrows() {
        DailyReportAgent failing = message -> {
            throw new IllegalStateException("模型限额用尽");
        };

        DailyReport report = serviceWith(failing).generate(DAY);

        assertThat(report.achievements()).containsExactly("修改优惠券领取逻辑");
    }

    @Test
    void fallsBackWhenAgentDoesNotSaveAnything() {
        DailyReportAgent chatty = message -> "我想了想，今天好像没什么好写的。";

        DailyReport report = serviceWith(chatty).generate(DAY);

        assertThat(report.achievements()).containsExactly("修改优惠券领取逻辑");
        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    void regeneratingKeepsSingleRecord() {
        ReportService service = serviceWith(null);
        service.generate(DAY);
        service.generate(DAY);

        assertThat(reportRepository.count()).isEqualTo(1);
    }

    @Test
    void noDataForTheDayIsRejected() {
        assertThatThrownBy(() -> serviceWith(null).generate(LocalDate.of(2020, 1, 1)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("没有任何活动记录");
    }
}
