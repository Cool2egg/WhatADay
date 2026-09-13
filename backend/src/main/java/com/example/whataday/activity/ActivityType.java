package com.example.whataday.activity;

/**
 * 活动类型（固定枚举）。
 *
 * <p>模型输出的 {@code type} 必须落在这些取值内；无法识别时统一降级为 {@link #OTHER}，
 * 以保证入库数据可统计、可筛选。
 */
public enum ActivityType {

    CODING("编码"),
    LEARNING("学习"),
    MEETING("会议"),
    BROWSING("浏览"),
    ENTERTAINMENT("娱乐"),
    COMMUNICATION("沟通"),
    OTHER("其他");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    /** 中文名，用于日志、工具输出等面向人的场合。JSON 序列化仍用枚举名（CODING 等）。 */
    public String label() {
        return label;
    }

    /** 大小写不敏感的宽松解析，未知值一律返回 {@link #OTHER}。 */
    public static ActivityType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
