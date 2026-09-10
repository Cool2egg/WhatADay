package com.example.whataday.report;

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

/** {@code daily_report} 表的数据访问：每日日报。 */
@Repository
public class ReportRepository {

    private static final String SELECT_ALL = """
            SELECT report_date, timeline_json, achievements_json, learning_json,
                   distractions_json, next_actions_json, created_at, updated_at
            FROM daily_report
            """;

    /**
     * 以 {@code report_date} 为冲突键的 upsert。
     *
     * <p>关键点：冲突时只更新内容与 {@code updated_at}，<b>不更新 {@code created_at}</b>，
     * 从而保留「首次生成时间」；这让重复生成日报天然幂等——同一天永远只有一行。
     */
    private static final String UPSERT = """
            INSERT INTO daily_report
                (report_date, timeline_json, achievements_json, learning_json,
                 distractions_json, next_actions_json, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(report_date) DO UPDATE SET
                timeline_json     = excluded.timeline_json,
                achievements_json = excluded.achievements_json,
                learning_json     = excluded.learning_json,
                distractions_json = excluded.distractions_json,
                next_actions_json = excluded.next_actions_json,
                updated_at        = excluded.updated_at
            """;

    private final JdbcTemplate jdbc;
    private final JsonColumn json;

    public ReportRepository(JdbcTemplate jdbc, JsonColumn json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 新增或更新某一天的日报；同一日期重复调用是幂等的。 */
    public void upsert(DailyReport report) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = report.createdAt() != null ? report.createdAt() : now;
        LocalDateTime updatedAt = report.updatedAt() != null ? report.updatedAt() : now;
        jdbc.update(UPSERT,
                report.reportDate().toString(),
                json.write(report.timeline()),
                json.write(report.achievements()),
                json.write(report.learning()),
                json.write(report.distractions()),
                json.write(report.nextActions()),
                Timestamps.format(createdAt),
                Timestamps.format(updatedAt));
    }

    public Optional<DailyReport> findByDate(LocalDate date) {
        return jdbc.query(SELECT_ALL + " WHERE report_date = ?", this::mapRow, date.toString())
                .stream().findFirst();
    }

    /** 全部日报，按日期倒序（前端历史日报列表）。 */
    public List<DailyReport> findAll() {
        return jdbc.query(SELECT_ALL + " ORDER BY report_date DESC", this::mapRow);
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM daily_report", Integer.class);
        return count == null ? 0 : count;
    }

    private DailyReport mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DailyReport(
                LocalDate.parse(rs.getString("report_date")),
                json.readList(rs.getString("timeline_json")),
                json.readList(rs.getString("achievements_json")),
                json.readList(rs.getString("learning_json")),
                json.readList(rs.getString("distractions_json")),
                json.readList(rs.getString("next_actions_json")),
                Timestamps.parse(rs.getString("created_at")),
                Timestamps.parse(rs.getString("updated_at"))
        );
    }
}
