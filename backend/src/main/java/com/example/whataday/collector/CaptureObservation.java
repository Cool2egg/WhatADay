package com.example.whataday.collector;

import java.time.LocalDateTime;

/**
 * 采集器观察到的一次「事实」，尚未经过模型理解。
 *
 * <p>它记录的是客观信息（前台应用、窗口标题、时间），
 * 活动语义由后续的 {@code VisionActivityAnalyzer} 生成。
 *
 * @param id             主键，插入前为 {@code null}
 * @param observedAt     观察时间
 * @param appName        前台应用名
 * @param windowTitle    窗口标题
 * @param analysisStatus 分析状态
 * @param analysisError  分析失败时的错误信息，可为 {@code null}
 * @param createdAt      入库时间
 */
public record CaptureObservation(
        Long id,
        LocalDateTime observedAt,
        String appName,
        String windowTitle,
        AnalysisStatus analysisStatus,
        String analysisError,
        LocalDateTime createdAt
) {

    public CaptureObservation {
        if (analysisStatus == null) {
            analysisStatus = AnalysisStatus.PENDING;
        }
    }
}
