package com.example.whataday.memory;

import com.example.whataday.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkMemoryRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private WorkMemoryRepository repository;

    @Test
    void upsertIsIdempotentBySource() {
        LocalDate day = LocalDate.of(2026, 9, 10);
        repository.upsert(memory(day, "第一次总结", "hash-1"));
        repository.upsert(memory(day, "更新后的总结", "hash-2"));

        assertThat(repository.count()).isEqualTo(1);
        WorkMemory saved = repository.findBySource("DAILY_REPORT", "2026-09-10")
                .orElseThrow();
        assertThat(saved.content()).isEqualTo("更新后的总结");
        assertThat(saved.contentHash()).isEqualTo("hash-2");
    }

    private static WorkMemory memory(LocalDate day, String content, String hash) {
        return new WorkMemory(null, "DAILY_REPORT", day.toString(), day, content,
                List.of("日报"), hash, null, null, null);
    }
}
