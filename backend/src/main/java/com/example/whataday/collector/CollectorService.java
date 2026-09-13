package com.example.whataday.collector;

/**
 * 桌面采集能力。
 *
 * <p>抽象出接口是为了让「采集方式」可替换：M2 由 {@link MockCollectorService}（Mock 模式）实现，
 * M4 再补上基于 JNA + AWT Robot 的 Windows 实现，业务层与前端无需改动。
 */
public interface CollectorService {

    /** 当前状态。 */
    CollectorStatus status();

    /** 开始采集；已在运行时不重复启动（幂等）。 */
    CollectorStatus start();

    /** 停止采集；未运行时不报错（幂等）。 */
    CollectorStatus stop();

    /** 立即采集一次，返回本次新增的采集条数（0 表示窗口未变化或未读到窗口）。 */
    int captureNow();
}
