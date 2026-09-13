package com.example.whataday.collector;

import com.example.whataday.activity.ActivityEvent;
import com.example.whataday.activity.ActivitySource;
import com.example.whataday.activity.ActivityType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityMergerTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 9, 13, 10, 0);
    private static final String APP = "idea64";

    private final ActivityMerger merger = new ActivityMerger(new CollectorProperties());

    @Test
    void mergesAdjacentEventsWithSameAppTitleAndType() {
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), APP, "CouponService.java", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(5), BASE.plusMinutes(6), APP, "CouponService.java", ActivityType.CODING);

        assertThat(merger.canMerge(existing, incoming)).isTrue();
    }

    @Test
    void doesNotMergeAcrossDifferentType() {
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), APP, "CouponService.java", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(5), BASE.plusMinutes(6), APP, "CouponService.java", ActivityType.BROWSING);

        assertThat(merger.canMerge(existing, incoming)).isFalse();
    }

    @Test
    void doesNotMergeAcrossDifferentTitleOrApp() {
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), APP, "CouponService.java", ActivityType.CODING);

        assertThat(merger.canMerge(existing,
                event(BASE.plusMinutes(5), BASE.plusMinutes(6), APP, "Other.java", ActivityType.CODING))).isFalse();
        assertThat(merger.canMerge(existing,
                event(BASE.plusMinutes(5), BASE.plusMinutes(6), "chrome", "CouponService.java", ActivityType.CODING))).isFalse();
    }

    @Test
    void ignoresCaseAndWhitespaceWhenComparing() {
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), "IDEA64", " CouponService.java ", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(5), BASE.plusMinutes(6), "idea64", "CouponService.java", ActivityType.CODING);

        assertThat(merger.canMerge(existing, incoming)).isTrue();
    }

    @Test
    void doesNotMergeWhenGapIsTooLarge() {
        // 默认允许的最大间隔是 5 分钟，这里断开 12 分钟
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), APP, "CouponService.java", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(17), BASE.plusMinutes(18), APP, "CouponService.java", ActivityType.CODING);

        assertThat(merger.canMerge(existing, incoming)).isFalse();
    }

    @Test
    void doesNotMergeBeyondMaxMergeMinutes() {
        // 默认单活动上限 30 分钟：间隔合法，但合并后会超过上限
        ActivityEvent existing = event(BASE, BASE.plusMinutes(30), APP, "CouponService.java", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(31), BASE.plusMinutes(32), APP, "CouponService.java", ActivityType.CODING);

        assertThat(merger.canMerge(existing, incoming)).isFalse();
    }

    @Test
    void mergeKeepsExistingIdentityAndOnlyAdvancesEndTime() {
        ActivityEvent existing = event(BASE, BASE.plusMinutes(5), APP, "CouponService.java", ActivityType.CODING);
        ActivityEvent incoming = event(BASE.plusMinutes(6), BASE.plusMinutes(8), APP, "CouponService.java", ActivityType.CODING);

        ActivityEvent merged = merger.merge(existing, incoming);

        assertThat(merged.id()).isEqualTo(existing.id());
        assertThat(merged.startTime()).isEqualTo(existing.startTime());
        assertThat(merged.endTime()).isEqualTo(BASE.plusMinutes(8));
        assertThat(merged.description()).isEqualTo(existing.description());
        assertThat(merged.type()).isEqualTo(existing.type());
    }

    private static ActivityEvent event(LocalDateTime start, LocalDateTime end,
                                       String app, String title, ActivityType type) {
        return new ActivityEvent(1L, null, start, end, app, title, type,
                "描述", List.of("kw"), 0.9, ActivitySource.VISION, null);
    }
}
