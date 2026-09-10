package com.example.whataday.activity;

/**
 * 活动类型（固定枚举）。
 *
 * <p>模型输出的 {@code type} 必须落在这些取值内；无法识别时统一降级为 {@link #OTHER}，
 * 以保证入库数据可统计、可筛选。
 */
public enum ActivityType {
    CODING,
    LEARNING,
    MEETING,
    BROWSING,
    ENTERTAINMENT,
    COMMUNICATION,
    OTHER;

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
