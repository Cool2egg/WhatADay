package com.example.whataday.report;

import com.example.whataday.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReportRepository repository;

    @Test
    void upsertTwiceKeepsSingleRowAndUpdatesContent() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        LocalDateTime firstRun = LocalDateTime.of(2026, 9, 10, 22, 0, 0);

        // 第一次生成
        repository.upsert(new DailyReport(date,
                List.of("10:00-12:00 写优惠券代码"),
                List.of("完成领取逻辑"),
                List.of(),
                List.of(),
                List.of("补充单元测试"),
                firstRun, firstRun));

        // 同一天重新生成：应更新内容，但只保留一行
        LocalDateTime secondRun = LocalDateTime.of(2026, 9, 10, 23, 30, 0);
        repository.upsert(new DailyReport(date,
                List.of("10:00-12:00 写优惠券代码", "14:00-15:00 组会"),
                List.of("完成领取逻辑", "同步了进度"),
                List.of("LangChain4j Tool Calling"),
                List.of("刷了 20 分钟短视频"),
                List.of("写日报模板"),
                secondRun, secondRun));

        assertThat(repository.count()).isEqualTo(1);

        DailyReport saved = repository.findByDate(date).orElseThrow();
        assertThat(saved.timeline()).containsExactly("10:00-12:00 写优惠券代码", "14:00-15:00 组会");
        assertThat(saved.achievements()).containsExactly("完成领取逻辑", "同步了进度");
        assertThat(saved.learning()).containsExactly("LangChain4j Tool Calling");
        assertThat(saved.distractions()).containsExactly("刷了 20 分钟短视频");
        assertThat(saved.nextActions()).containsExactly("写日报模板");

        // 幂等的关键：createdAt 保留首次生成时间，updatedAt 刷新为最后一次
        assertThat(saved.createdAt()).isEqualTo(firstRun);
        assertThat(saved.updatedAt()).isEqualTo(secondRun);
    }

    @Test
    void findByDateReturnsEmptyForUnknownDay() {
        assertThat(repository.findByDate(LocalDate.of(2026, 1, 1))).isEmpty();
    }

    @Test
    void findAllReturnsReportsInDescendingDateOrder() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 10, 22, 0, 0);
        repository.upsert(new DailyReport(LocalDate.of(2026, 9, 9), List.of(), List.of(), List.of(), List.of(), List.of(), now, now));
        repository.upsert(new DailyReport(LocalDate.of(2026, 9, 10), List.of(), List.of(), List.of(), List.of(), List.of(), now, now));

        assertThat(repository.findAll())
                .extracting(DailyReport::reportDate)
                .containsExactly(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 9));
    }
}
