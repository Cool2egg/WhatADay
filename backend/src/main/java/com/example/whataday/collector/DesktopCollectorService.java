package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivityService;
import com.example.whataday.activity.ActivitySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 真实桌面采集器：读取前台窗口、按需截图、产出活动事件。
 *
 * <p>它自己不含任何平台专有代码——读窗口和截图分别委托给 {@link ActiveWindowReader}
 * 与 {@link ScreenshotCapturer}，Windows 实现只在 {@code mode=desktop} 时装配。
 * 这样采集流程本身可以在任何平台上被完整测试。
 *
 * <p>单次采集的顺序（对应方案第 5.1、8 节）：
 * <ol>
 *   <li>读取前台窗口</li>
 *   <li><b>先做敏感应用过滤</b>——命中就直接返回，绝不截图、绝不调用模型</li>
 *   <li>窗口没变则只推进当前活动的结束时间，不产生新观察</li>
 *   <li>窗口变了才落一条观察，并按节流规则决定是否截图</li>
 *   <li>分析 → 落库或并入相邻活动；截图在 {@code finally} 中删除</li>
 * </ol>
 */
@Service
@ConditionalOnProperty(name = "whataday.collector.mode", havingValue = "desktop")
public class DesktopCollectorService implements CollectorService {

    private static final Logger log = LoggerFactory.getLogger(DesktopCollectorService.class);

    private final ActiveWindowReader windowReader;
    private final ScreenshotCapturer screenshotCapturer;
    private final PrivacyFilter privacyFilter;
    private final CapturePolicy capturePolicy;
    private final CaptureScheduler scheduler;
    private final ActivityAnalyzer analyzer;
    private final ActivityMerger merger;
    private final ObservationRepository observationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final CollectorProperties properties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile WindowInfo lastWindow;
    private volatile LocalDateTime lastScreenshotAt;
    private volatile LocalDateTime startedAt;
    private volatile LocalDateTime lastCaptureAt;

    public DesktopCollectorService(ActiveWindowReader windowReader,
                                   ScreenshotCapturer screenshotCapturer,
                                   PrivacyFilter privacyFilter,
                                   CapturePolicy capturePolicy,
                                   CaptureScheduler scheduler,
                                   ActivityAnalyzer analyzer,
                                   ActivityMerger merger,
                                   ObservationRepository observationRepository,
                                   ActivityRepository activityRepository,
                                   ActivityService activityService,
                                   CollectorProperties properties) {
        this.windowReader = windowReader;
        this.screenshotCapturer = screenshotCapturer;
        this.privacyFilter = privacyFilter;
        this.capturePolicy = capturePolicy;
        this.scheduler = scheduler;
        this.analyzer = analyzer;
        this.merger = merger;
        this.observationRepository = observationRepository;
        this.activityRepository = activityRepository;
        this.activityService = activityService;
        this.properties = properties;
    }

    @Override
    public CollectorStatus status() {
        LocalDate today = LocalDate.now();
        ActivitySummary summary = activityService.summary(today, null);
        return new CollectorStatus(
                running.get(),
                properties.getMode(),
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
            // 复位窗口记忆，保证启动后第一次轮询一定产生观察
            resetWindowMemory();
            scheduler.start(this::pollOnce, Duration.ofSeconds(properties.getWindowPollSeconds()));
            log.info("开始桌面采集：轮询间隔 {} 秒，截图间隔 {} 秒，敏感应用 {} 个",
                    properties.getWindowPollSeconds(),
                    properties.getScreenshotMinIntervalSeconds(),
                    privacyFilter.excludedProcesses().size());
        }
        return status();
    }

    /**
     * 清空「上次看到的窗口」。
     *
     * <p>开始采集时调用：这样重启采集不会因为「窗口看起来没变」而漏掉第一次观察。
     * 注意这不会造成重复活动——重新观察到的内容会被合并规则并回已有活动，
     * 所以「停止再开始」不会把一段时间线切成两条。
     */
    synchronized void resetWindowMemory() {
        lastWindow = null;
    }

    @Override
    public synchronized CollectorStatus stop() {
        running.set(false);
        scheduler.stop();
        return status();
    }

    @Override
    public int captureNow() {
        return pollOnce();
    }

    /**
     * 采集一次。返回本次是否记录了新的观察（1 = 有，0 = 窗口未变化 / 未读到窗口）。
     *
     * <p>加锁是必要的：定时线程与「立即采集」接口可能同时进入。
     */
    synchronized int pollOnce() {
        Optional<WindowInfo> maybeWindow = windowReader.readForegroundWindow();
        if (maybeWindow.isEmpty()) {
            return 0;
        }
        WindowInfo current = maybeWindow.get();
        LocalDateTime now = LocalDateTime.now();

        // 窗口没有变化：不产生新观察，只把当前活动的结束时间推进到现在
        if (!capturePolicy.shouldObserve(lastWindow, current)) {
            extendCurrentActivity(current, now);
            return 0;
        }
        lastWindow = current;

        // 隐私过滤发生在截图之前，这是整个设计里最要紧的一条顺序
        if (privacyFilter.isExcluded(current.processName())) {
            observationRepository.insert(new CaptureObservation(
                    null, now, current.appName(), current.windowTitle(),
                    AnalysisStatus.IGNORED, "命中敏感应用黑名单，未截图、未调用模型", null));
            log.debug("命中敏感应用黑名单，本次仅记录忽略状态");
            return 1;
        }

        long observationId = observationRepository.insert(new CaptureObservation(
                null, now, current.appName(), current.windowTitle(),
                AnalysisStatus.PENDING, null, null));

        Optional<Path> screenshot = Optional.empty();
        try {
            if (capturePolicy.shouldCaptureScreenshot(lastScreenshotAt, now)) {
                screenshot = screenshotCapturer.capture();
                if (screenshot.isPresent()) {
                    lastScreenshotAt = now;
                }
            }

            CaptureObservation observation = observationRepository.findById(observationId).orElseThrow();
            ActivityEvent produced = analyzer.analyze(observation, screenshot);
            saveOrMerge(produced);

            observationRepository.updateAnalysisStatus(observationId, AnalysisStatus.ANALYZED, null);
            lastCaptureAt = now;
            return 1;
        } catch (RuntimeException e) {
            log.warn("分析采集结果失败，已标记为 FAILED：{}", e.toString());
            observationRepository.updateAnalysisStatus(observationId, AnalysisStatus.FAILED, e.toString());
            return 1;
        } finally {
            // 原始截图无论如何都要删掉：成功了不需要留，失败了更不该留
            screenshot.ifPresent(this::deleteQuietly);
        }
    }

    /** 窗口未变时推进当前活动的结束时间，让活动有真实时长而不是一堆零时长碎片。 */
    private void extendCurrentActivity(WindowInfo current, LocalDateTime now) {
        Optional<ActivityEvent> latest = activityRepository.findLatest(now.toLocalDate());
        if (latest.isEmpty()) {
            return;
        }
        ActivityEvent existing = latest.get();
        ActivityEvent probe = new ActivityEvent(
                null, null, now, now,
                current.appName(), current.windowTitle(),
                existing.type(), existing.description(), existing.keywords(),
                existing.confidence(), existing.source(), null);

        if (merger.canMerge(existing, probe)) {
            activityRepository.updateEndTime(existing.id(), now);
        }
    }

    private void saveOrMerge(ActivityEvent produced) {
        LocalDateTime startTime = produced.startTime() != null ? produced.startTime() : LocalDateTime.now();
        Optional<ActivityEvent> latest = activityRepository.findLatest(startTime.toLocalDate());

        if (latest.isPresent() && merger.canMerge(latest.get(), produced)) {
            activityRepository.updateEndTime(latest.get().id(), produced.endTime());
        } else {
            activityRepository.insert(produced);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除临时截图失败：{}", path.getFileName());
        }
    }
}
