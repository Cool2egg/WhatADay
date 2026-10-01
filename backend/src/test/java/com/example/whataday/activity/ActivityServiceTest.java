package com.example.whataday.activity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    @Test
    void metricsAggregatesFocusDistractionAndTypeSwitches() {
        ActivityRepository repository = mock(ActivityRepository.class);
        LocalDate day = LocalDate.of(2026, 9, 10);
        when(repository.findByDateRange(day, day)).thenReturn(List.of(
                event(day, 9, 0, 10, 30, ActivityType.CODING),
                event(day, 10, 30, 11, 0, ActivityType.BROWSING),
                event(day, 11, 0, 12, 0, ActivityType.LEARNING)));

        WorkMetrics metrics = new ActivityService(repository).metrics(day, day);

        assertThat(metrics.eventCount()).isEqualTo(3);
        assertThat(metrics.activeMinutes()).isEqualTo(180);
        assertThat(metrics.focusedMinutes()).isEqualTo(150);
        assertThat(metrics.distractionMinutes()).isEqualTo(30);
        assertThat(metrics.typeSwitchCount()).isEqualTo(2);
        assertThat(metrics.minutesByType()).containsEntry(ActivityType.CODING, 90L)
                .containsEntry(ActivityType.BROWSING, 30L)
                .containsEntry(ActivityType.LEARNING, 60L);
    }

    @Test
    void metricsRejectsReversedDateRange() {
        ActivityRepository repository = mock(ActivityRepository.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new ActivityService(repository).metrics(
                        LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("结束日期不能早于开始日期");
    }

    private static ActivityEvent event(LocalDate day, int startHour, int startMinute,
                                       int endHour, int endMinute, ActivityType type) {
        LocalDateTime start = day.atTime(startHour, startMinute);
        return new ActivityEvent(null, null, start, day.atTime(endHour, endMinute),
                "App", "Window", type, type.label(), List.of(), 1.0,
                ActivitySource.WINDOW_FALLBACK, null);
    }
}
