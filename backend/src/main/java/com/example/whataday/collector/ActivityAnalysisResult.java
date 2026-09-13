package com.example.whataday.collector;

import java.util.List;

/**
 * 视觉模型返回的结构化结果。
 *
 * <p>对应提示词里约定好的 JSON 结构。字段刻意都用可空的包装类型：
 * 模型可能漏字段或给出多余字段，解析成 {@code null} 比直接抛异常更可控——
 * 缺什么就用窗口信息补什么。
 */
public record ActivityAnalysisResult(
        String appName,
        String windowTitle,
        String type,
        String description,
        List<String> keywords,
        Double confidence
) {

    public ActivityAnalysisResult {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
