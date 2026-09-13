package com.example.whataday.api;

import com.example.whataday.ApiTestSupport;
import com.example.whataday.activity.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CollectorApiTest extends ApiTestSupport {

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void statusReportsStoppedModeAndCounters() throws Exception {
        mockMvc.perform(get("/api/collector/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mode").value("mock"))
                .andExpect(jsonPath("$.data.running").value(false))
                .andExpect(jsonPath("$.data.todayActivityCount").value(0));
    }

    @Test
    void captureNowInsertsOneActivityAndReportsIt() throws Exception {
        mockMvc.perform(post("/api/collector/capture-now"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        assertThat(activityRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/api/collector/status"))
                .andExpect(jsonPath("$.data.lastCaptureAt").isNotEmpty())
                .andExpect(jsonPath("$.data.todayActivityCount").value(1));
    }
}
