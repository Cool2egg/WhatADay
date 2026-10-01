package com.example.whataday.memory;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** 工作记忆召回：优先向量相似度，无向量时退化为关键词匹配。 */
@Service
public class WorkMemorySearchService {

    private final WorkMemoryRepository repository;
    private final Optional<EmbeddingModel> embeddingModel;

    public WorkMemorySearchService(WorkMemoryRepository repository,
                                   Optional<EmbeddingModel> embeddingModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
    }

    public List<MemoryHit> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<WorkMemory> memories = repository.findAll();
        float[] queryVector = embed(query);
        return memories.stream()
                .map(memory -> new MemoryHit(memory, score(query, queryVector, memory)))
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(MemoryHit::score).reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
    }

    private double score(String query, float[] queryVector, WorkMemory memory) {
        if (queryVector != null && memory.embeddingJson() != null) {
            return cosine(queryVector, parseVector(memory.embeddingJson()));
        }
        String content = memory.content().toLowerCase();
        return query.codePoints().filter(Character::isLetterOrDigit)
                .mapToDouble(codePoint -> content.indexOf(codePoint) >= 0 ? 1.0 : 0.0)
                .sum();
    }

    private float[] embed(String query) {
        if (embeddingModel.isEmpty()) return null;
        try {
            return embeddingModel.get().embed(query).content().vector();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static double cosine(float[] left, float[] right) {
        if (left.length != right.length) return 0;
        double dot = 0, leftNorm = 0, rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        return leftNorm == 0 || rightNorm == 0
                ? 0 : dot / Math.sqrt(leftNorm * rightNorm);
    }

    private static float[] parseVector(String json) {
        String body = json.substring(1, json.length() - 1).trim();
        if (body.isEmpty()) return new float[0];
        String[] values = body.split(",");
        float[] vector = new float[values.length];
        for (int i = 0; i < values.length; i++) vector[i] = Float.parseFloat(values[i]);
        return vector;
    }

    public record MemoryHit(WorkMemory memory, double score) {
    }
}
