package com.example.whataday.api;

import com.example.whataday.ApiTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import com.example.whataday.report.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiTest extends ApiTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 10);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void insertActivity() {
        insertActivity(ActivityType.CODING, "改优惠券领取逻辑");
    }

    @Test
    void generateThenFetch() throws Exception {
        mockMvc.perform(post("/api/reports/2026-09-10/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportDate").value("2026-09-10"))
                .andExpect(jsonPath("$.data.achievements[0]").value("改优惠券领取逻辑"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        mockMvc.perform(get("/api/reports/2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportDate").value("2026-09-10"));
    }

    @Test
    void fetchingMissingReportReturns404() throws Exception {
        mockMvc.perform(get("/api/reports/2020-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generatingReportWithoutAnyRecordReturns404() throws Exception {
        mockMvc.perform(post("/api/reports/2020-01-01/generate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void regeneratingUpdatesTheSameSingleRecord() throws Exception {
        mockMvc.perform(post("/api/reports/2026-09-10/generate")).andExpect(status().isOk());

        // 再补一条学习活动，然后重新生成
        insertActivity(ActivityType.LEARNING, "学习 LangChain4j");
        mockMvc.perform(post("/api/reports/2026-09-10/generate")).andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(reportRepository.count()).isEqualTo(1);
        mockMvc.perform(get("/api/reports/2026-09-10"))
                .andExpect(jsonPath("$.data.learning[0]").value("学习 LangChain4j"));
    }

    @Test
    void listReturnsReports() throws Exception {
        mockMvc.perform(post("/api/reports/2026-09-10/generate")).andExpect(status().isOk());

        mockMvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reportDate").value("2026-09-10"));
    }

    private void insertActivity(ActivityType type, String description) {
        activityRepository.insert(new ActivityEvent(null, null,
                LocalDateTime.of(DAY, LocalTime.of(10, 0)),
                LocalDateTime.of(DAY, LocalTime.of(11, 0)),
                "IntelliJ IDEA", "CouponService.java", type, description,
                List.of("Java"), 0.9, ActivitySource.VISION, null));
    }
}
