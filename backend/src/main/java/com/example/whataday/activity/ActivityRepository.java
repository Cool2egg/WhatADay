package com.example.whataday.activity;

import com.example.whataday.common.JsonColumn;
import com.example.whataday.common.Timestamps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** {@code activity_event} 表的数据访问：结构化活动事件。 */
@Repository
public class ActivityRepository {

    private static final String SELECT_ALL = """
            SELECT id, observation_id, start_time, end_time, app_name, window_title,
                   type, description, keywords_json, confidence, source, created_at
            FROM activity_event
            """;

    private final JdbcTemplate jdbc;
    private final JsonColumn json;

    public ActivityRepository(JdbcTemplate jdbc, JsonColumn json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 插入一条活动事件，返回自增主键。 */
    public long insert(ActivityEvent event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO activity_event
                        (observation_id, start_time, end_time, app_name, window_title,
                         type, description, keywords_json, confidence, source, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, event.observationId());
            ps.setString(2, Timestamps.format(event.startTime()));
            ps.setString(3, Timestamps.format(event.endTime()));
            ps.setString(4, event.appName());
            ps.setString(5, event.windowTitle());
            ps.setString(6, event.type().name());
            ps.setString(7, event.description());
            ps.setString(8, json.write(event.keywords()));
            ps.setObject(9, event.confidence());
            ps.setString(10, event.source().name());
            ps.setString(11, Timestamps.format(
                    event.createdAt() != null ? event.createdAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<ActivityEvent> findById(long id) {
        return jdbc.query(SELECT_ALL + " WHERE id = ?", this::mapRow, id).stream().findFirst();
    }

    /** 查询某一天的全部活动，按开始时间升序。 */
    public List<ActivityEvent> findByDate(LocalDate date) {
        return jdbc.query(
                SELECT_ALL + " WHERE start_time >= ? AND start_time < ? ORDER BY start_time",
                this::mapRow, Timestamps.dayStart(date), Timestamps.dayEnd(date));
    }

    /** 查询某一天、指定类型的活动（前端时间线的类型筛选）。 */
    public List<ActivityEvent> findByDateAndType(LocalDate date, ActivityType type) {
        return jdbc.query(
                SELECT_ALL + " WHERE start_time >= ? AND start_time < ? AND type = ? ORDER BY start_time",
                this::mapRow, Timestamps.dayStart(date), Timestamps.dayEnd(date), type.name());
    }

    public int deleteById(long id) {
        return jdbc.update("DELETE FROM activity_event WHERE id = ?", id);
    }

    /** 某一天最近的一条活动，用于判断新事件能否并入（合并逻辑）。 */
    public Optional<ActivityEvent> findLatest(LocalDate date) {
        return jdbc.query(
                SELECT_ALL + " WHERE start_time >= ? AND start_time < ? ORDER BY start_time DESC LIMIT 1",
                this::mapRow, Timestamps.dayStart(date), Timestamps.dayEnd(date))
                .stream().findFirst();
    }

    /** 推进某条活动的结束时间（合并时使用）。 */
    public int updateEndTime(long id, LocalDateTime endTime) {
        return jdbc.update("UPDATE activity_event SET end_time = ? WHERE id = ?",
                Timestamps.format(endTime), id);
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM activity_event", Integer.class);
        return count == null ? 0 : count;
    }

    private ActivityEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        double confidence = rs.getDouble("confidence");
        Double confidenceOrNull = rs.wasNull() ? null : confidence;

        long observationId = rs.getLong("observation_id");
        Long observationIdOrNull = rs.wasNull() ? null : observationId;

        return new ActivityEvent(
                rs.getLong("id"),
                observationIdOrNull,
                Timestamps.parse(rs.getString("start_time")),
                Timestamps.parse(rs.getString("end_time")),
                rs.getString("app_name"),
                rs.getString("window_title"),
                ActivityType.from(rs.getString("type")),
                rs.getString("description"),
                json.readList(rs.getString("keywords_json")),
                confidenceOrNull,
                ActivitySource.valueOf(rs.getString("source")),
                Timestamps.parse(rs.getString("created_at"))
        );
    }
}
