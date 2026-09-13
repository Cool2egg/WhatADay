package com.example.whataday.collector;

import java.time.LocalDateTime;

/**
 * 采集器当前状态（前端「今日工作台」轮询展示）。
 *
 * @param running                是否正在采集
 * @param mode                   采集模式：mock / windows
 * @param startedAt              本次采集的开始时间，未运行为 {@code null}
 * @param lastCaptureAt          最近一次立即采集的时间，未采集过为 {@code null}
 * @param todayObservationCount  今日采集到的观察数
 * @param todayActivityCount     今日生成的活动事件数
 * @param todayActiveMinutes     今日活动累计时长（分钟）
 */
public record CollectorStatus(
        boolean running,
        String mode,
        LocalDateTime startedAt,
        LocalDateTime lastCaptureAt,
        long todayObservationCount,
        long todayActivityCount,
        long todayActiveMinutes
) {
}
