package com.example.whataday.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集器配置（对应 {@code whataday.collector.*}）。
 *
 * <p>把方案里那些「每 30 秒 / 每 2 分钟 / 最多合并 30 分钟」的硬编码数字提成配置，
 * 既是让它们可调，也是为了让测试能构造出需要的时间窗口。
 */
@Component
@ConfigurationProperties(prefix = "whataday.collector")
public class CollectorProperties {

    /** mock 或 desktop。 */
    private String mode = "mock";

    /** 敏感应用黑名单：命中则完全不截图、不调用模型。 */
    private List<String> excludedProcesses = new ArrayList<>();

    /** 读取前台窗口的间隔（秒）。 */
    private int windowPollSeconds = 30;

    /** 两次截图之间的最小间隔（秒），控制模型成本与隐私暴露面。 */
    private int screenshotMinIntervalSeconds = 120;

    /** 单个活动最多合并到多少分钟。 */
    private int maxMergeMinutes = 30;

    /** 相邻事件之间允许的最大间隔（分钟），超过则不合并。 */
    private int mergeGapMinutes = 5;

    /** 截图缩放后的最大宽度（像素），用于降低体积与传输成本。 */
    private int screenshotMaxWidth = 1280;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<String> getExcludedProcesses() {
        return excludedProcesses;
    }

    public void setExcludedProcesses(List<String> excludedProcesses) {
        this.excludedProcesses = excludedProcesses;
    }

    public int getWindowPollSeconds() {
        return windowPollSeconds;
    }

    public void setWindowPollSeconds(int windowPollSeconds) {
        this.windowPollSeconds = windowPollSeconds;
    }

    public int getScreenshotMinIntervalSeconds() {
        return screenshotMinIntervalSeconds;
    }

    public void setScreenshotMinIntervalSeconds(int screenshotMinIntervalSeconds) {
        this.screenshotMinIntervalSeconds = screenshotMinIntervalSeconds;
    }

    public int getMaxMergeMinutes() {
        return maxMergeMinutes;
    }

    public void setMaxMergeMinutes(int maxMergeMinutes) {
        this.maxMergeMinutes = maxMergeMinutes;
    }

    public int getMergeGapMinutes() {
        return mergeGapMinutes;
    }

    public void setMergeGapMinutes(int mergeGapMinutes) {
        this.mergeGapMinutes = mergeGapMinutes;
    }

    public int getScreenshotMaxWidth() {
        return screenshotMaxWidth;
    }

    public void setScreenshotMaxWidth(int screenshotMaxWidth) {
        this.screenshotMaxWidth = screenshotMaxWidth;
    }
}
