package com.example.whataday.api;

import com.example.whataday.ApiTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.report.ReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 幂等性专项测试。
 *
 * <p>「所有 start、stop 和日报生成接口必须幂等」是 PROJECT_PLAN.md 第 7 节的硬性要求，
 * 这里把它作为一条独立契约来验证，而不是散落在各处顺带一测。
 */
class IdempotencyTest extends ApiTestSupport {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void startingCollectorTwiceDoesNotDuplicateData() throws Exception {
        mockMvc.perform(post("/api/collector/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true));

        long afterFirstStart = activityRepository.count();

        mockMvc.perform(post("/api/collector/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true));

        assertThat(activityRepository.count()).isEqualTo(afterFirstStart);
    }

    @Test
    void stoppingCollectorTwiceIsSafe() throws Exception {
        mockMvc.perform(post("/api/collector/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));

        mockMvc.perform(post("/api/collector/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));
    }

    @Test
    void generatingTodayReportTwiceKeepsSingleRecord() throws Exception {
        // 自备数据，不依赖 Mock 灌数，保证用例独立且确定
        LocalDate today = LocalDate.now();
        activityRepository.insert(new ActivityEvent(null, null,
                today.atTime(10, 0), today.atTime(11, 0),
                "IntelliJ IDEA", "CouponService.java", ActivityType.CODING, "改优惠券领取逻辑",
                List.of("Java"), 0.9, ActivitySource.VISION, null));

        mockMvc.perform(post("/api/reports/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportDate").value(today.toString()));

        mockMvc.perform(post("/api/reports/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportDate").value(today.toString()));

        assertThat(reportRepository.count()).isEqualTo(1);
    }
}
