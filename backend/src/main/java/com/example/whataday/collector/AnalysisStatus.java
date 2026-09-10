package com.example.whataday.collector;

/**
 * 采集观察的分析状态。
 *
 * <ul>
 *   <li>{@link #PENDING} —— 已采集，等待分析</li>
 *   <li>{@link #ANALYZED} —— 已完成分析（视觉模型或降级）</li>
 *   <li>{@link #FAILED} —— 分析失败</li>
 *   <li>{@link #IGNORED} —— 命中敏感应用黑名单，不截图、不调用模型</li>
 * </ul>
 */
public enum AnalysisStatus {
    PENDING,
    ANALYZED,
    FAILED,
    IGNORED
}
