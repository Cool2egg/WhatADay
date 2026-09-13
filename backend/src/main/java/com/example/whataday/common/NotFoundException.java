package com.example.whataday.common;

import org.springframework.http.HttpStatus;

/** 资源不存在，对应 HTTP 404。 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
