package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 把一次采集观察理解成结构化活动事件。
 *
 * <p>抽象成接口是为了让「理解方式」可替换：
 * M4 只有 {@link WindowFallbackAnalyzer}（用窗口信息降级），
 * M5 会加入基于视觉模型的实现，并在模型失败时回退到同一个降级实现。
 *
 * @see WindowFallbackAnalyzer
 */
public interface ActivityAnalyzer {

    /**
     * @param observation 已落库的采集观察
     * @param screenshot  本次可用的临时截图；没有截图时为空
     * @return 结构化活动事件（{@code id} 为 {@code null}，由调用方决定是插入还是合并）
     */
    ActivityEvent analyze(CaptureObservation observation, Optional<Path> screenshot);
}
