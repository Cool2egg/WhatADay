package com.example.whataday.collector;

import com.example.whataday.RepositoryTestSupport;
import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivityRepository;
import com.example.whataday.activity.ActivityService;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 采集流程的测试。
 *
 * <p>关键是：读窗口与截图这两个平台专有动作被替换成了桩，
 * 于是「隐私过滤的顺序」「截图必被删除」「节流」「合并」这些真正重要的行为，
 * 全部可以在 Linux 上验证，不需要 Windows 环境。真正依赖 Windows 的只剩两个适配器本身的实现。
 */
class DesktopCollectorServiceTest extends RepositoryTestSupport {

    private static final WindowInfo IDEA = new WindowInfo("idea64.exe", "CouponService.java");
    private static final WindowInfo CHROME = new WindowInfo("chrome.exe", "掘金 - 首页");
    private static final WindowInfo WECHAT = new WindowInfo("WeChat.exe", "微信");

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityService activityService;

    private final StubWindowReader windowReader = new StubWindowReader();
    private final StubScreenshotCapturer screenshotCapturer = new StubScreenshotCapturer();

    private DesktopCollectorService serviceWith(ActivityAnalyzer analyzer) {
        CollectorProperties properties = new CollectorProperties();
        properties.setMode("desktop");
        properties.setExcludedProcesses(List.of("WeChat.exe"));
        properties.setScreenshotMinIntervalSeconds(120);
        properties.setMaxMergeMinutes(30);
        properties.setMergeGapMinutes(5);

        return new DesktopCollectorService(
                windowReader,
                screenshotCapturer,
                new PrivacyFilter(properties),
                new CapturePolicy(properties),
                new CaptureScheduler(),
                analyzer,
                new ActivityMerger(properties),
                observationRepository,
                activityRepository,
                activityService,
                properties);
    }

    private DesktopCollectorService service() {
        return serviceWith(new WindowFallbackAnalyzer());
    }

    @Test
    void firstCaptureRecordsObservationAndFallbackActivity() {
        windowReader.set(IDEA);
        DesktopCollectorService service = service();

        assertThat(service.captureNow()).isEqualTo(1);

        assertThat(observationRepository.count()).isEqualTo(1);

        ActivityEvent activity = activityRepository.findLatest(LocalDate.now()).orElseThrow();
        assertThat(activity.appName()).isEqualTo("idea64");
        assertThat(activity.windowTitle()).isEqualTo("CouponService.java");
        // M4 还没有视觉模型，所有活动都走窗口降级
        assertThat(activity.type()).isEqualTo(ActivityType.OTHER);
        assertThat(activity.source()).isEqualTo(ActivitySource.WINDOW_FALLBACK);
    }

    @Test
    void unchangedWindowDoesNotProduceAnotherObservation() {
        windowReader.set(IDEA);
        DesktopCollectorService service = service();

        service.captureNow();

        assertThat(service.captureNow()).isZero();
        assertThat(observationRepository.count()).isEqualTo(1);
        assertThat(activityRepository.count()).isEqualTo(1);
    }

    @Test
    void sensitiveAppIsFilteredBeforeAnyScreenshot() {
        windowReader.set(WECHAT);
        DesktopCollectorService service = service();

        service.captureNow();

        // 最要紧的一条：命中敏感应用时绝不截图、也不产出活动
        assertThat(screenshotCapturer.createdFiles()).isEmpty();
        assertThat(activityRepository.count()).isZero();

        assertThat(observationRepository.findByDate(LocalDate.now()))
                .singleElement()
                .satisfies(observation ->
                        assertThat(observation.analysisStatus()).isEqualTo(AnalysisStatus.IGNORED));
    }

    @Test
    void screenshotIsDeletedAfterAnalysis() {
        windowReader.set(IDEA);
        DesktopCollectorService service = service();

        service.captureNow();

        assertThat(screenshotCapturer.createdFiles()).hasSize(1);
        assertThat(Files.exists(screenshotCapturer.createdFiles().get(0))).isFalse();
    }

    @Test
    void screenshotIsDeletedEvenWhenAnalysisThrows() {
        windowReader.set(IDEA);
        DesktopCollectorService service = serviceWith((observation, screenshot) -> {
            throw new IllegalStateException("模型不可用");
        });

        service.captureNow();

        // 分析失败时原始截图更不该留在磁盘上
        assertThat(screenshotCapturer.createdFiles()).hasSize(1);
        assertThat(Files.exists(screenshotCapturer.createdFiles().get(0))).isFalse();

        assertThat(activityRepository.count()).isZero();
        assertThat(observationRepository.findByDate(LocalDate.now()))
                .singleElement()
                .satisfies(observation ->
                        assertThat(observation.analysisStatus()).isEqualTo(AnalysisStatus.FAILED));
    }

    @Test
    void secondScreenshotIsThrottledWithinMinimumInterval() {
        DesktopCollectorService service = service();

        windowReader.set(IDEA);
        service.captureNow();
        windowReader.set(CHROME);
        service.captureNow();

        // 观察照记，但两分钟内的第二张截图被节流
        assertThat(observationRepository.count()).isEqualTo(2);
        assertThat(screenshotCapturer.createdFiles()).hasSize(1);
    }

    @Test
    void unchangedWindowAdvancesCurrentActivityEndTime() {
        windowReader.set(IDEA);
        DesktopCollectorService service = service();

        // 第一次采集：建立窗口记忆，并产生一条活动
        service.captureNow();
        ActivityEvent created = activityRepository.findLatest(LocalDate.now()).orElseThrow();

        // 把它的结束时间往回拨三分钟，模拟「用户已经在同一个窗口待了一会儿」
        LocalDateTime backdated = LocalDateTime.now().minusMinutes(3).withNano(0);
        activityRepository.updateEndTime(created.id(), backdated);

        // 窗口没变：不产生新观察，但当前活动的结束时间要推进到现在
        assertThat(service.captureNow()).isZero();

        assertThat(observationRepository.count()).isEqualTo(1);
        assertThat(activityRepository.count()).isEqualTo(1);

        ActivityEvent after = activityRepository.findLatest(LocalDate.now()).orElseThrow();
        assertThat(after.id()).isEqualTo(created.id());
        assertThat(after.endTime()).isAfter(backdated);
    }

    @Test
    void reObservingSameWindowMergesInsteadOfDuplicating() {
        windowReader.set(IDEA);
        DesktopCollectorService service = service();

        service.captureNow();
        assertThat(activityRepository.count()).isEqualTo(1);

        // 相当于「停止采集后再次开始」：窗口记忆被清空，于是又产生一次观察
        service.resetWindowMemory();
        service.captureNow();

        assertThat(observationRepository.count()).isEqualTo(2);
        // 但时间线没有被切成两条
        assertThat(activityRepository.count()).isEqualTo(1);
    }

    @Test
    void noForegroundWindowProducesNothing() {
        DesktopCollectorService service = service();
        // 桩未设置窗口，等同于读不到前台窗口

        assertThat(service.captureNow()).isZero();
        assertThat(observationRepository.count()).isZero();
    }

    // ------------------------------------------------------------------
    // 测试替身：替代 Windows 专有实现，让采集流程可以在任意平台被完整验证
    // ------------------------------------------------------------------

    private static final class StubWindowReader implements ActiveWindowReader {

        private WindowInfo current;

        void set(WindowInfo info) {
            this.current = info;
        }

        @Override
        public Optional<WindowInfo> readForegroundWindow() {
            return Optional.ofNullable(current);
        }
    }

    private static final class StubScreenshotCapturer implements ScreenshotCapturer {

        private final List<Path> created = new ArrayList<>();

        List<Path> createdFiles() {
            return created;
        }

        @Override
        public Optional<Path> capture() {
            try {
                Path file = Files.createTempFile("whataday-test-shot-", ".png");
                Files.writeString(file, "fake-image");
                created.add(file);
                return Optional.of(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
