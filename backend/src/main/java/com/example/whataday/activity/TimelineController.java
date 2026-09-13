package com.example.whataday.activity;

import com.example.whataday.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 活动时间线查询接口。 */
@RestController
@RequestMapping("/api/timeline")
@Tag(name = "时间线", description = "按日期与活动类型查询活动事件")
public class TimelineController {

    private final ActivityService activityService;

    public TimelineController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/today")
    @Operation(summary = "今日活动时间线")
    public ApiResponse<List<ActivityEvent>> today() {
        return ApiResponse.ok(activityService.today());
    }

    @GetMapping
    @Operation(summary = "按日期查询时间线",
            description = "date 缺省为今天；type 缺省表示不筛选。type 取值见 ActivityType 枚举")
    public ApiResponse<List<ActivityEvent>> byDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ActivityType type) {
        return ApiResponse.ok(activityService.timeline(date, type));
    }

    @GetMapping("/summary")
    @Operation(summary = "当日活动统计",
            description = "返回活动数量、累计时长，以及各类型的数量分布（含数量为 0 的类型）")
    public ApiResponse<ActivitySummary> summary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) ActivityType type) {
        return ApiResponse.ok(activityService.summary(date, type));
    }
}
