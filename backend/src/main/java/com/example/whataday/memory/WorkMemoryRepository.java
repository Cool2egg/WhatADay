package com.example.whataday.memory;

import com.example.whataday.common.JsonColumn;
import com.example.whataday.common.Timestamps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 工作记忆持久化，按来源幂等更新，后续可在此基础上增加向量检索。 */
@Repository
public class WorkMemoryRepository {

    private static final String SELECT_ALL = """
            SELECT id, source_type, source_key, memory_date, content, tags_json,
                   content_hash, embedding_json, created_at, updated_at
            FROM work_memory
            """;

    private final JdbcTemplate jdbc;
    private final JsonColumn json;

    public WorkMemoryRepository(JdbcTemplate jdbc, JsonColumn json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 同一来源重复索引只更新内容，不产生重复记忆。 */
    public void upsert(WorkMemory memory) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO work_memory
                    (source_type, source_key, memory_date, content, tags_json,
                     content_hash, embedding_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(source_type, source_key) DO UPDATE SET
                    memory_date = excluded.memory_date,
                    content = excluded.content,
                    tags_json = excluded.tags_json,
                    content_hash = excluded.content_hash,
                    embedding_json = excluded.embedding_json,
                    updated_at = excluded.updated_at
                """,
                memory.sourceType(), memory.sourceKey(), memory.memoryDate().toString(),
                memory.content(), json.write(memory.tags()), memory.contentHash(),
                memory.embeddingJson(),
                Timestamps.format(memory.createdAt() != null ? memory.createdAt() : now),
                Timestamps.format(now));
    }

    public Optional<WorkMemory> findBySource(String sourceType, String sourceKey) {
        return jdbc.query(SELECT_ALL + " WHERE source_type = ? AND source_key = ?",
                this::mapRow, sourceType, sourceKey).stream().findFirst();
    }

    public List<WorkMemory> findByDate(LocalDate date) {
        return jdbc.query(SELECT_ALL + " WHERE memory_date = ? ORDER BY id",
                this::mapRow, date.toString());
    }

    public List<WorkMemory> findAll() {
        return jdbc.query(SELECT_ALL + " ORDER BY memory_date DESC, id DESC", this::mapRow);
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM work_memory", Integer.class);
        return count == null ? 0 : count;
    }

    private WorkMemory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new WorkMemory(
                rs.getLong("id"),
                rs.getString("source_type"),
                rs.getString("source_key"),
                LocalDate.parse(rs.getString("memory_date")),
                rs.getString("content"),
                json.readList(rs.getString("tags_json")),
                rs.getString("content_hash"),
                rs.getString("embedding_json"),
                Timestamps.parse(rs.getString("created_at")),
                Timestamps.parse(rs.getString("updated_at")));
    }
}
