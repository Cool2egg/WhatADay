package com.example.whataday.config;

/**
 * 模型相关的装配条件。
 *
 * <p>抽成常量是为了让「有没有配置 API Key」这个判断在所有装配点保持完全一致，
 * 避免两处条件写法不同导致行为不一致。
 */
public final class ModelConditions {

    /**
     * 已配置视觉/语言模型 API Key。
     *
     * <p>用 {@code @ConditionalOnExpression} 而不是 {@code @ConditionalOnProperty}：
     * Key 未设置时属性值是空字符串，而 {@code @ConditionalOnProperty} 会把
     * 「属性存在但为空」判为匹配，结果就是拿着空 Key 去构建模型并在启动时报错。
     */
    public static final String API_KEY_PRESENT = "'${whataday.vision.api-key:}'.length() > 0";

    private ModelConditions() {
    }
}
