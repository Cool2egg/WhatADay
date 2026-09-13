package com.example.whataday.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * <p>业务代码只负责抛出「哪里不对」，由 {@link GlobalExceptionHandler} 统一决定
 * HTTP 状态码与响应体格式，避免异常处理逻辑散落在各个 Controller 里。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
