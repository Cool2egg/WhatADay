package com.example.whataday.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 统一响应包装。
 *
 * <p>所有接口都返回这一层壳，前端只需判断 {@code success} 并读取 {@code data}，
 * 不必为每个接口写不同的解析逻辑；出错时 {@code data} 为 {@code null}、{@code message} 有说明。
 *
 * @param success   是否成功
 * @param data      业务数据，失败时为 {@code null}
 * @param message   错误信息，成功时为 {@code null}
 * @param timestamp 服务端响应时间
 */
@Schema(description = "统一响应包装")
public record ApiResponse<T>(
        @Schema(description = "是否成功", example = "true") boolean success,
        @Schema(description = "业务数据，失败时为 null") T data,
        @Schema(description = "错误信息，成功时为 null") String message,
        @Schema(description = "服务端响应时间") LocalDateTime timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message, LocalDateTime.now());
    }
}
