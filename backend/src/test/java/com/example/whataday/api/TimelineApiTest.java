package com.example.whataday.api;

import com.example.whataday.ApiTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimelineApiTest extends ApiTestSupport {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 10);

    @Autowired
    private ActivityRepository activityRepository;

    @BeforeEach
    void insertFixture() {
        activityRepository.insert(new ActivityEvent(null, null, at(10, 0), at(10, 30),
                "IntelliJ IDEA", "CouponService.java", ActivityType.CODING, "改优惠券领取逻辑",
                List.of("Java", "优惠券"), 0.93, ActivitySource.VISION, null));
        activityRepository.insert(new ActivityEvent(null, null, at(14, 0), at(14, 20),
                "Chrome", "掘金 - 优惠券系统设计", ActivityType.BROWSING, "浏览技术文章",
                List.of("掘金"), 0.81, ActivitySource.VISION, null));
    }

    @Test
    void byDateReturnsTimelineWithKeywords() throws Exception {
        mockMvc.perform(get("/api/timeline").param("date", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].type").value("CODING"))
                .andExpect(jsonPath("$.data[0].startTime").value("2026-09-10T10:00:00"))
                .andExpect(jsonPath("$.data[0].keywords[1]").value("优惠券"));
    }

    @Test
    void typeFilterNarrowsResult() throws Exception {
        mockMvc.perform(get("/api/timeline")
                        .param("date", "2026-09-10")
                        .param("type", "BROWSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("BROWSING"));
    }

    @Test
    void unknownTypeIsRejectedWith400() throws Exception {
        mockMvc.perform(get("/api/timeline")
                        .param("date", "2026-09-10")
                        .param("type", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void summaryAggregatesCountsAndDuration() throws Exception {
        mockMvc.perform(get("/api/timeline/summary").param("date", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventCount").value(2))
                // 30 分钟 + 20 分钟
                .andExpect(jsonPath("$.data.activeMinutes").value(50))
                .andExpect(jsonPath("$.data.countByType.CODING").value(1))
                // 未出现的类型也返回 0，前端可直接画图
                .andExpect(jsonPath("$.data.countByType.MEETING").value(0));
    }

    @Test
    void todayReturnsOkEvenWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/timeline/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(DAY, LocalTime.of(hour, minute));
    }
}
