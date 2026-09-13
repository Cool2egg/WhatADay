package com.example.whataday.collector;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 采集定时器。
 *
 * <p>用自己持有的单线程调度器而不是 {@code @Scheduled}，是因为采集必须能被
 * 「开始 / 停止」按钮立即控制：{@code @Scheduled} 到点照样触发，还得在方法里额外判运行标志，
 * 不如直接在启停时创建 / 关闭调度器来得干净。
 *
 * <p>线程设为 daemon，避免应用关闭时被采集线程挂住。
 */
@Component
public class CaptureScheduler {

    private ScheduledExecutorService executor;

    /** 按固定间隔重复执行任务；重复调用会先停掉上一个调度。 */
    public synchronized void start(Runnable task, Duration interval) {
        stop();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "whataday-capture");
            thread.setDaemon(true);
            return thread;
        });
        long seconds = Math.max(1, interval.getSeconds());
        executor.scheduleWithFixedDelay(task, 0, seconds, TimeUnit.SECONDS);
    }

    /** 停止调度；未运行时安全无副作用（幂等）。 */
    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }
}
