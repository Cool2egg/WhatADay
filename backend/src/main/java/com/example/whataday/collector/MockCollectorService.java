package com.example.whataday.collector;

import com.example.whataday.activity.ActivityService;
import com.example.whataday.activity.ActivitySummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock 模式的采集器实现。
 *
 * <p>它不真的读取窗口或截图，而是用 {@link MockDataSeeder} 提供的数据模拟采集结果，
 * 用于没有 Windows 环境或模型 API Key 时的开发与演示。
 * M4 会加入基于 JNA + AWT Robot 的 Windows 实现，本类保持不变。
 *
 * <p>start / stop 都是幂等的：重复调用不会产生额外副作用，状态始终一致。
 */
@Service
@ConditionalOnProperty(name = "whataday.collector.mode", havingValue = "mock", matchIfMissing = true)
public class MockCollectorService implements CollectorService {

    private final MockDataSeeder seeder;
    private final ActivityService activityService;
    private final ObservationRepository observationRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime lastCaptureAt;

    public MockCollectorService(MockDataSeeder seeder,
                                ActivityService activityService,
                                ObservationRepository observationRepository) {
        this.seeder = seeder;
        this.activityService = activityService;
        this.observationRepository = observationRepository;
    }

    @Override
    public CollectorStatus status() {
        LocalDate today = LocalDate.now();
        ActivitySummary summary = activityService.summary(today, null);
        return new CollectorStatus(
                running.get(),
                "mock",
                startedAt,
                lastCaptureAt,
                observationRepository.findByDate(today).size(),
                summary.eventCount(),
                summary.activeMinutes());
    }

    @Override
    public synchronized CollectorStatus start() {
        if (running.compareAndSet(false, true)) {
            startedAt = LocalDateTime.now();
            // Mock 模式下补上当天示例数据，让前端一开始就有内容可看
            seeder.seedIfEmpty(LocalDate.now());
        }
        return status();
    }

    @Override
    public synchronized CollectorStatus stop() {
        running.set(false);
        return status();
    }

    @Override
    public synchronized int captureNow() {
        int inserted = seeder.appendCurrentActivity();
        lastCaptureAt = LocalDateTime.now();
        return inserted;
    }
}
