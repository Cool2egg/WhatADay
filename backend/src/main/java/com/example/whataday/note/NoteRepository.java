package com.example.whataday.note;

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

/** {@code user_note} 表的数据访问：用户手动记录。 */
@Repository
public class NoteRepository {

    private static final String SELECT_ALL =
            "SELECT id, note_time, content, tags_json, created_at FROM user_note";

    private final JdbcTemplate jdbc;
    private final JsonColumn json;

    public NoteRepository(JdbcTemplate jdbc, JsonColumn json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** 插入一条手动记录，返回自增主键。 */
    public long insert(UserNote note) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO user_note (note_time, content, tags_json, created_at)
                    VALUES (?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, Timestamps.format(note.noteTime()));
            ps.setString(2, note.content());
            ps.setString(3, json.write(note.tags()));
            ps.setString(4, Timestamps.format(
                    note.createdAt() != null ? note.createdAt() : LocalDateTime.now()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<UserNote> findById(long id) {
        return jdbc.query(SELECT_ALL + " WHERE id = ?", this::mapRow, id).stream().findFirst();
    }

    /** 查询某一天的全部手动记录，按记录时间升序。 */
    public List<UserNote> findByDate(LocalDate date) {
        return jdbc.query(
                SELECT_ALL + " WHERE note_time >= ? AND note_time < ? ORDER BY note_time",
                this::mapRow, Timestamps.dayStart(date), Timestamps.dayEnd(date));
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM user_note", Integer.class);
        return count == null ? 0 : count;
    }

    private UserNote mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new UserNote(
                rs.getLong("id"),
                Timestamps.parse(rs.getString("note_time")),
                rs.getString("content"),
                json.readList(rs.getString("tags_json")),
                Timestamps.parse(rs.getString("created_at"))
        );
    }
}
