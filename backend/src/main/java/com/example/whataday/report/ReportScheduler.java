package com.example.whataday.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 日报定时任务：每天 22:00 自动生成当天日报（对应 PROJECT_PLAN 第 5.3 节）。
 *
 * <p>cron 表达式做成可配置的，是为了演示时能临时改成每分钟跑一次，
 * 不用等到晚上十点才能看到效果。
 */
@Component
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportService reportService;

    public ReportScheduler(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(cron = "${whataday.report.cron:0 0 22 * * *}")
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("定时任务触发：生成 {} 的工作日报", today);
        try {
            DailyReport report = reportService.generate(today);
            log.info("日报生成完成：{}，时间线 {} 条、成果 {} 条、下一步 {} 条",
                    report.reportDate(),
                    report.timeline().size(),
                    report.achievements().size(),
                    report.nextActions().size());
        } catch (Exception e) {
            // 定时任务里让异常逃出去会影响后续调度，这里只记录，下一个周期照常触发
            log.warn("定时生成日报失败：{}", e.toString());
        }
    }
}
