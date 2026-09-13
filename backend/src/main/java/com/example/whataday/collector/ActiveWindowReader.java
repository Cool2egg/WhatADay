package com.example.whataday.collector;

import java.util.Optional;

/**
 * 读取当前前台窗口。
 *
 * <p>抽象成接口有两个目的：一是把 Windows 专有调用隔离在实现里，
 * 让采集逻辑本身可以在任何平台上被测试；二是为将来其它平台留出扩展点。
 */
public interface ActiveWindowReader {

    /** 读取当前前台窗口；无法读取（无权限、无前台窗口、平台不支持）时返回空。 */
    Optional<WindowInfo> readForegroundWindow();
}
