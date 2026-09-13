package com.example.whataday.collector;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * 采集策略：什么时候该产生新观察、什么时候该截图。
 *
 * <p>这两条判断决定了采集的「分贝」——它们直接关系到模型调用成本和隐私暴露面，
 * 所以抽成独立的、可单独测试的纯逻辑，而不是散在采集循环里。
 */
@Component
public class CapturePolicy {

    private final CollectorProperties properties;

    public CapturePolicy(CollectorProperties properties) {
        this.properties = properties;
    }

    /**
     * 是否需要产生一条新的观察。
     *
     * <p>方案第 5.1 节：第一次采集、或前台窗口发生变化时才触发新观察。
     * 窗口没变时不是什么都不做——由采集服务去推进当前活动的结束时间。
     */
    public boolean shouldObserve(WindowInfo previous, WindowInfo current) {
        return previous == null || !sameWindow(previous, current);
    }

    /**
     * 是否允许截图。
     *
     * <p>两次截图之间强制间隔 {@code screenshotMinIntervalSeconds}（默认 2 分钟）。
     * 截图是成本最高、隐私风险最大的动作，必须限流。
     */
    public boolean shouldCaptureScreenshot(LocalDateTime lastScreenshotAt, LocalDateTime now) {
        if (lastScreenshotAt == null) {
            return true;
        }
        return Duration.between(lastScreenshotAt, now).getSeconds() >= properties.getScreenshotMinIntervalSeconds();
    }

    /** 两个窗口是否可视为同一个：进程名与标题都相同（忽略大小写与首尾空格）。 */
    public boolean sameWindow(WindowInfo left, WindowInfo right) {
        if (left == null || right == null) {
            return false;
        }
        return sameText(left.processName(), right.processName())
                && sameText(left.windowTitle(), right.windowTitle());
    }

    private static boolean sameText(String left, String right) {
        return Objects.equals(normalize(left), normalize(right));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
