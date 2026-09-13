package com.example.whataday.collector;

import java.util.Locale;

/**
 * 一次前台窗口读取的结果。
 *
 * @param processName 进程名，如 {@code idea64.exe}；用于敏感应用过滤
 * @param windowTitle 窗口标题，如 {@code CouponService.java}
 */
public record WindowInfo(String processName, String windowTitle) {

    /** 展示用应用名：去掉 {@code .exe} 后缀。 */
    public String appName() {
        if (processName == null || processName.isBlank()) {
            return "未知应用";
        }
        String lower = processName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".exe")
                ? processName.substring(0, processName.length() - 4)
                : processName;
    }
}
