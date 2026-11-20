package com.heartbeat.dao;

import com.heartbeat.model.entity.CheckinDiary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CheckinDiaryDao {

    private final JdbcTemplate jdbc;
    public CheckinDiaryDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<CheckinDiary> MAPPER = (rs, n) -> {
        CheckinDiary d = new CheckinDiary();
        d.setId(rs.getLong("id"));
        d.setRecordId(rs.getLong("record_id"));
        d.setUserId(rs.getLong("user_id"));
        d.setContent(rs.getString("content"));
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return d;
    };

    public void insert(CheckinDiary d) {
        jdbc.update("INSERT INTO checkin_diary(record_id, user_id, content) VALUES(?,?,?)",
                d.getRecordId(), d.getUserId(), d.getContent());
    }

    /** 个人日记：联表带出本次专注时长 */
    public List<java.util.Map<String, Object>> listWithDuration(Long userId, int limit) {
        String sql = "SELECT d.id, d.content, d.created_at AS createdAt, f.duration_seconds AS durationSeconds, " +
                "f.mode, r.name AS roomName " +
                "FROM checkin_diary d JOIN focus_record f ON d.record_id = f.id " +
                "LEFT JOIN room r ON f.room_id = r.id " +
                "WHERE d.user_id = ? ORDER BY d.created_at DESC LIMIT ?";
        return jdbc.queryForList(sql, userId, limit);
    }
}
