package com.example.whataday.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新增手动记录的请求体。
 *
 * @param noteTime 记录时间，不传则使用当前时间
 * @param content  记录内容
 * @param tags     标签，参与日报的「下一步行动」提取（标签为「下一步」的记录）
 */
@Schema(description = "新增手动记录请求")
public record CreateNoteRequest(
        @Schema(description = "记录时间，不传则取当前时间", example = "2026-09-10T12:05:00")
        LocalDateTime noteTime,

        @Schema(description = "记录内容", example = "上午把优惠券领取逻辑改完了")
        @NotBlank(message = "不能为空")
        @Size(max = 2000, message = "长度不能超过 2000")
        String content,

        @Schema(description = "标签", example = "[\"完成\"]")
        List<String> tags
) {
}
