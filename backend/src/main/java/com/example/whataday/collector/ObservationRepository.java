package com.example.whataday.collector;

import com.example.whataday.common.Timestamps;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** {@code capture_observation} 表的数据访问：采集器观察到的事实。 */
@Repository
public class ObservationRepository {

    private static final String SELECT_ALL = """
            SELECT id, observed_at, app_name, window_title,
                   analysis_status, analysis_error, created_at
            FROM capture_observation
            """;

    private final JdbcTemplate jdbc;

    private final RowMapper<CaptureObservation> rowMapper = (rs, rowNum) -> new CaptureObservation(
            rs.getLong("id"),
            Timestamps.parse(rs.getString("observed_at")),
            rs.getString("app_name"),
            rs.getString("window_title"),
            AnalysisStatus.valueOf(rs.getString("analysis_status")),
            rs.getString("analysis_error"),
            Timestamps.parse(rs.getString("created_at"))
    );

    public ObservationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 插入一条观察记录，返回自增主键。 */
    public long insert(CaptureObservation observation) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO capture_observation
                        (observed_at, app_name, window_title, analysis_status, analysis_error, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, Timestamps.format(observation.observedAt()));
            ps.setString(2, observation.appName());
            ps.setString(3, observation.windowTitle());
            ps.setString(4, observation.analysisStatus().name());
            ps.setString(5, observation.analysisError());
            ps.setString(6, Timestamps.format(
                    observation.createdAt() != null ? observation.createdAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<CaptureObservation> findById(long id) {
        return jdbc.query(SELECT_ALL + " WHERE id = ?", rowMapper, id).stream().findFirst();
    }

    /** 查询某一天（本地时区自然日）的全部观察记录，按观察时间升序。 */
    public List<CaptureObservation> findByDate(LocalDate date) {
        return jdbc.query(
                SELECT_ALL + " WHERE observed_at >= ? AND observed_at < ? ORDER BY observed_at",
                rowMapper, Timestamps.dayStart(date), Timestamps.dayEnd(date));
    }

    /** 更新某条观察记录的分析状态（采集器分析流程使用）。 */
    public int updateAnalysisStatus(long id, AnalysisStatus status, String error) {
        return jdbc.update(
                "UPDATE capture_observation SET analysis_status = ?, analysis_error = ? WHERE id = ?",
                status.name(), error, id);
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM capture_observation", Integer.class);
        return count == null ? 0 : count;
    }
}
