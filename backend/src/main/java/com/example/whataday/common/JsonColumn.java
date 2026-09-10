package com.example.whataday.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 以 {@code _json} 结尾的列的读写工具。
 *
 * <p>SQLite 没有数组类型，关键词、标签、日报各段都序列化成 JSON 文本存储。
 * 这里统一封装，保证读写两侧的格式一致（都用 Spring 容器里的同一个 {@link ObjectMapper}）。
 */
@Component
public class JsonColumn {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonColumn(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 把字符串列表序列化为 JSON 文本；{@code null} 视为空列表。 */
    public String write(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 JSON 列失败", e);
        }
    }

    /** 把 JSON 文本反序列化为字符串列表；{@code null} / 空串返回空列表。 */
    public List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析 JSON 列失败：" + json, e);
        }
    }
}
