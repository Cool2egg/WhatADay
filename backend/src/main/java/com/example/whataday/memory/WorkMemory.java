package com.example.whataday.memory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 一条可供工作助手检索的长期工作记忆。 */
public record WorkMemory(
        Long id,
        String sourceType,
        String sourceKey,
        LocalDate memoryDate,
        String content,
        List<String> tags,
        String contentHash,
        String embeddingJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public WorkMemory {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
