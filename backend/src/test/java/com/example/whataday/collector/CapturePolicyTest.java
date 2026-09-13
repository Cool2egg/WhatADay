package com.example.whataday.collector;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CapturePolicyTest {

    private static final WindowInfo IDEA = new WindowInfo("idea64.exe", "CouponService.java");

    private static CapturePolicy policyOf(int screenshotMinIntervalSeconds) {
        CollectorProperties properties = new CollectorProperties();
        properties.setScreenshotMinIntervalSeconds(screenshotMinIntervalSeconds);
        return new CapturePolicy(properties);
    }

    @Test
    void firstObservationIsAlwaysNeeded() {
        assertThat(policyOf(120).shouldObserve(null, IDEA)).isTrue();
    }

    @Test
    void newObservationOnlyWhenWindowChanges() {
        CapturePolicy policy = policyOf(120);

        assertThat(policy.shouldObserve(IDEA, new WindowInfo("idea64.exe", "CouponService.java"))).isFalse();
        assertThat(policy.shouldObserve(IDEA, new WindowInfo("idea64.exe", "Another.java"))).isTrue();
        assertThat(policy.shouldObserve(IDEA, new WindowInfo("chrome.exe", "CouponService.java"))).isTrue();
    }

    @Test
    void windowComparisonIgnoresCaseAndWhitespace() {
        CapturePolicy policy = policyOf(120);

        assertThat(policy.sameWindow(
                new WindowInfo("IDEA64.EXE", " CouponService.java "),
                new WindowInfo("idea64.exe", "CouponService.java"))).isTrue();
        assertThat(policy.sameWindow(null, IDEA)).isFalse();
    }

    @Test
    void firstScreenshotIsAlwaysAllowed() {
        assertThat(policyOf(120).shouldCaptureScreenshot(null, LocalDateTime.of(2026, 9, 13, 10, 0))).isTrue();
    }

    @Test
    void screenshotIsThrottledByMinimumInterval() {
        CapturePolicy policy = policyOf(120);
        LocalDateTime last = LocalDateTime.of(2026, 9, 13, 10, 0);

        assertThat(policy.shouldCaptureScreenshot(last, last.plusSeconds(119))).isFalse();
        assertThat(policy.shouldCaptureScreenshot(last, last.plusSeconds(120))).isTrue();
        assertThat(policy.shouldCaptureScreenshot(last, last.plusMinutes(10))).isTrue();
    }
}
