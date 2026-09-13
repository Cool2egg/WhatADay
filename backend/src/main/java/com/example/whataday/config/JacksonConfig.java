package com.example.whataday.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * 统一 API 中 {@link java.time.LocalDateTime} 的序列化格式。
 *
 * <p>Jackson 默认用 ISO-8601 输出，在有纳秒时写成 {@code 2026-09-13T17:35:01.480824987}，
 * 整秒时又省略小数位，导致同一批响应里时间格式不一致、前端还得兼容两种形态。
 * 这里固定为 {@code yyyy-MM-dd'T'HH:mm:ss}——与数据库里的存储格式保持一致，
 * 前端只按一种格式解析即可。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeFormatCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(DATE_TIME));
            builder.deserializers(new LocalDateTimeDeserializer(DATE_TIME));
        };
    }
}
