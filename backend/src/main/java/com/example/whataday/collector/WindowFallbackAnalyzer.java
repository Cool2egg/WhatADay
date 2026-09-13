package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 仅凭窗口信息生成活动事件——即方案里说的「模型失败时的降级路径」。
 *
 * <p>M4 还没有接入视觉模型，所以这条路径暂时是唯一路径：产出的活动类型一律为
 * {@link ActivityType#OTHER}，描述直接取窗口标题，置信度为空，来源标记为
 * {@link ActivitySource#WINDOW_FALLBACK}。等 M5 接入模型后，本类就回到它设计中的位置——
 * 模型不可用时的兜底，保证采集链路永远不会因为模型问题而断掉。
 */
@Component
public class WindowFallbackAnalyzer implements ActivityAnalyzer {

    @Override
    public ActivityEvent analyze(CaptureObservation observation, Optional<Path> screenshot) {
        String title = observation.windowTitle();
        String description = (title == null || title.isBlank())
                ? "使用 " + observation.appName()
                : title;

        return new ActivityEvent(
                null,
                observation.id(),
                observation.observedAt(),
                // 初始为零时长；用户停留在同一窗口时，由合并逻辑持续推进结束时间
                observation.observedAt(),
                observation.appName(),
                title,
                ActivityType.OTHER,
                description,
                List.of(),
                null,
                ActivitySource.WINDOW_FALLBACK,
                null);
    }
}
