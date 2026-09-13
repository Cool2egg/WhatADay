package com.example.whataday.report;

import com.example.whataday.common.ApiResponse;
import com.example.whataday.common.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 日报接口。 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "日报", description = "查看 / 生成 / 重新生成每日日报")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(summary = "历史日报列表", description = "按日期倒序")
    public ApiResponse<List<DailyReport>> list() {
        return ApiResponse.ok(reportService.list());
    }

    @PostMapping("/today")
    @Operation(summary = "生成今日日报", description = "幂等：重复调用只会更新同一条记录")
    public ApiResponse<DailyReport> generateToday() {
        return ApiResponse.ok(reportService.generate(LocalDate.now()));
    }

    @PostMapping("/{date}/generate")
    @Operation(summary = "生成指定日期日报", description = "用于补生成或重新生成，同样是幂等的")
    public ApiResponse<DailyReport> generate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.generate(date));
    }

    @GetMapping("/{date}")
    @Operation(summary = "查看指定日期日报", description = "不存在时返回 404")
    public ApiResponse<DailyReport> get(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(reportService.find(date)
                .orElseThrow(() -> new NotFoundException("还没有 " + date + " 的日报")));
    }
}
