package com.example.whataday.memory;

import com.example.whataday.note.UserNote;
import com.example.whataday.report.DailyReport;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** 将日报和手动记录写入工作记忆，并在 Embedding 可用时生成向量。 */
@Service
public class WorkMemoryIndexer {

    private final WorkMemoryRepository repository;
    private final Optional<EmbeddingModel> embeddingModel;

    public WorkMemoryIndexer(WorkMemoryRepository repository,
                             Optional<EmbeddingModel> embeddingModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
    }

    public void indexReport(DailyReport report) {
        String content = joinSections(
                "日报 " + report.reportDate(),
                section("时间线", report.timeline()),
                section("成果", report.achievements()),
                section("学习", report.learning()),
                section("分心", report.distractions()),
                section("下一步", report.nextActions()));
        upsert("DAILY_REPORT", report.reportDate().toString(), report.reportDate(), content,
                List.of("日报"));
    }

    public void indexNote(UserNote note) {
        LocalDate date = note.noteTime().toLocalDate();
        String content = "手动记录 " + date + "\n" + note.content();
        upsert("USER_NOTE", String.valueOf(note.id()), date, content, note.tags());
    }

    private void upsert(String sourceType, String sourceKey, LocalDate date,
                        String content, List<String> tags) {
        String hash = sha256(content);
        String embeddingJson = repository.findBySource(sourceType, sourceKey)
                .filter(existing -> hash.equals(existing.contentHash()))
                .map(WorkMemory::embeddingJson)
                .orElseGet(() -> embed(content));
        repository.upsert(new WorkMemory(null, sourceType, sourceKey, date, content,
                tags, hash, embeddingJson, null, null));
    }

    private String embed(String content) {
        if (embeddingModel.isEmpty()) {
            return null;
        }
        try {
            Embedding embedding = embeddingModel.get().embed(content).content();
            return toJson(embedding.vector());
        } catch (RuntimeException ignored) {
            // Embedding 失败不影响原始工作记录和日报保存，后续可重新索引。
            return null;
        }
    }

    private static String section(String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return title + "：无";
        }
        return title + "：\n- " + String.join("\n- ", values);
    }

    private static String joinSections(String... sections) {
        return Stream.of(sections).filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private static String toJson(float[] vector) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) json.append(',');
            json.append(vector[i]);
        }
        return json.append(']').toString();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte each : digest) {
                result.append(String.format("%02x", each));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 不支持 SHA-256", e);
        }
    }
}
