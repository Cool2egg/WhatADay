package com.example.whataday.collector;

import com.example.whataday.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 采集器控制接口。 */
@RestController
@RequestMapping("/api/collector")
@Tag(name = "采集器", description = "开始 / 停止 / 查看状态 / 立即采集")
public class CollectorController {

    private final CollectorService collectorService;

    public CollectorController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @PostMapping("/start")
    @Operation(summary = "开始采集", description = "幂等：已在运行时不会重复启动，直接返回当前状态")
    public ApiResponse<CollectorStatus> start() {
        return ApiResponse.ok(collectorService.start());
    }

    @PostMapping("/stop")
    @Operation(summary = "停止采集", description = "幂等：未运行时不会报错，直接返回当前状态")
    public ApiResponse<CollectorStatus> stop() {
        return ApiResponse.ok(collectorService.stop());
    }

    @GetMapping("/status")
    @Operation(summary = "查看采集状态", description = "前端每 5 秒轮询一次")
    public ApiResponse<CollectorStatus> status() {
        return ApiResponse.ok(collectorService.status());
    }

    @PostMapping("/capture-now")
    @Operation(summary = "立即采集一次", description = "返回本次新增的活动事件数")
    public ApiResponse<Integer> captureNow() {
        return ApiResponse.ok(collectorService.captureNow());
    }
}
