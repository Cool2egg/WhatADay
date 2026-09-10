package com.example.whataday.collector;

import com.example.whataday.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ObservationRepository repository;

    @Test
    void insertThenFindById() {
        LocalDateTime observedAt = LocalDateTime.of(2026, 9, 10, 10, 0, 0);

        long id = repository.insert(new CaptureObservation(
                null, observedAt, "IntelliJ IDEA", "CouponService.java",
                AnalysisStatus.PENDING, null, null));

        CaptureObservation saved = repository.findById(id).orElseThrow();
        assertThat(saved.appName()).isEqualTo("IntelliJ IDEA");
        assertThat(saved.windowTitle()).isEqualTo("CouponService.java");
        assertThat(saved.observedAt()).isEqualTo(observedAt);
        assertThat(saved.analysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    }

    @Test
    void findByDateOnlyReturnsThatDay() {
        repository.insert(new CaptureObservation(null, LocalDateTime.of(2026, 9, 10, 9, 0),
                "Code", "A.java", AnalysisStatus.ANALYZED, null, null));
        repository.insert(new CaptureObservation(null, LocalDateTime.of(2026, 9, 10, 23, 59, 59),
                "Chrome", "B", AnalysisStatus.ANALYZED, null, null));
        repository.insert(new CaptureObservation(null, LocalDateTime.of(2026, 9, 11, 0, 0),
                "Chrome", "C", AnalysisStatus.ANALYZED, null, null));

        assertThat(repository.findByDate(LocalDate.of(2026, 9, 10))).hasSize(2);
        assertThat(repository.findByDate(LocalDate.of(2026, 9, 11))).hasSize(1);
        assertThat(repository.findByDate(LocalDate.of(2026, 9, 12))).isEmpty();
    }

    @Test
    void updateAnalysisStatusPersistsError() {
        long id = repository.insert(new CaptureObservation(null, LocalDateTime.of(2026, 9, 10, 10, 0),
                "WeChat", "微信", AnalysisStatus.PENDING, null, null));

        repository.updateAnalysisStatus(id, AnalysisStatus.IGNORED, "命中敏感应用黑名单");

        CaptureObservation saved = repository.findById(id).orElseThrow();
        assertThat(saved.analysisStatus()).isEqualTo(AnalysisStatus.IGNORED);
        assertThat(saved.analysisError()).isEqualTo("命中敏感应用黑名单");
    }
}
