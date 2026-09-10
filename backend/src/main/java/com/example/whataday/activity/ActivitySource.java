package com.example.whataday.activity;

/**
 * 活动事件的来源。
 *
 * <ul>
 *   <li>{@link #VISION} —— 视觉模型分析截图后生成</li>
 *   <li>{@link #WINDOW_FALLBACK} —— 模型调用失败时，仅用窗口信息降级生成</li>
 * </ul>
 */
public enum ActivitySource {
    VISION,
    WINDOW_FALLBACK
}
