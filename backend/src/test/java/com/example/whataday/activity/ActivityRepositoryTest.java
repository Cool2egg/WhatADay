package com.example.whataday.activity;

import com.example.whataday.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ActivityRepository repository;

    private static ActivityEvent codingEvent(LocalDateTime start, List<String> keywords, Double confidence) {
        return new ActivityEvent(null, null, start, start.plusMinutes(30),
                "IntelliJ IDEA", "CouponService.java", ActivityType.CODING,
                "正在修改优惠券领取逻辑", keywords, confidence, ActivitySource.VISION, null);
    }

    @Test
    void keywordsRoundTripThroughJsonColumn() {
        List<String> keywords = List.of("Java", "Spring Boot", "优惠券");
        long id = repository.insert(codingEvent(LocalDateTime.of(2026, 9, 10, 10, 0), keywords, 0.93));

        ActivityEvent saved = repository.findById(id).orElseThrow();
        assertThat(saved.keywords()).containsExactly("Java", "Spring Boot", "优惠券");
        assertThat(saved.type()).isEqualTo(ActivityType.CODING);
        assertThat(saved.source()).isEqualTo(ActivitySource.VISION);
        assertThat(saved.confidence()).isEqualTo(0.93);
    }

    @Test
    void confidenceIsClampedIntoZeroToOne() {
        long id = repository.insert(codingEvent(LocalDateTime.of(2026, 9, 10, 10, 0), List.of(), 1.7));

        assertThat(repository.findById(id).orElseThrow().confidence()).isEqualTo(1.0);
    }

    @Test
    void nullKeywordsAndConfidenceAreReadBackSafely() {
        long id = repository.insert(new ActivityEvent(null, null,
                LocalDateTime.of(2026, 9, 10, 11, 0), null, "Code.exe", "Main.java",
                ActivityType.CODING, "降级事件", null, null, ActivitySource.WINDOW_FALLBACK, null));

        ActivityEvent saved = repository.findById(id).orElseThrow();
        assertThat(saved.keywords()).isEmpty();
        assertThat(saved.confidence()).isNull();
        assertThat(saved.source()).isEqualTo(ActivitySource.WINDOW_FALLBACK);
    }

    @Test
    void findByDateAndTypeFiltersByType() {
        repository.insert(codingEvent(LocalDateTime.of(2026, 9, 10, 10, 0), List.of(), 0.9));
        repository.insert(new ActivityEvent(null, null, LocalDateTime.of(2026, 9, 10, 14, 0),
                LocalDateTime.of(2026, 9, 10, 14, 20), "Chrome", "掘金 - 首页",
                ActivityType.BROWSING, "浏览技术文章", List.of("掘金"), 0.8, ActivitySource.VISION, null));

        LocalDate day = LocalDate.of(2026, 9, 10);
        assertThat(repository.findByDate(day)).hasSize(2);
        assertThat(repository.findByDateAndType(day, ActivityType.CODING)).hasSize(1);
        assertThat(repository.findByDateAndType(day, ActivityType.BROWSING)).hasSize(1);
        assertThat(repository.findByDateAndType(day, ActivityType.MEETING)).isEmpty();
    }
}
