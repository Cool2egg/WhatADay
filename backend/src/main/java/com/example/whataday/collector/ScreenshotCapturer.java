package com.example.whataday.collector;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 截屏能力。
 *
 * <p>实现负责把图写到临时文件并返回路径；<b>删除由调用方负责</b>——
 * 采集服务用 {@code try/finally} 保证无论分析成功还是抛异常，原始截图都会被清理。
 */
public interface ScreenshotCapturer {

    /** 截取当前屏幕到临时文件；失败或环境不支持（如无图形界面）时返回空。 */
    Optional<Path> capture();
}
