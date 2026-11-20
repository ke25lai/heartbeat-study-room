package com.heartbeat.dao;

import com.heartbeat.model.entity.FocusRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class FocusRecordDao {

    private final JdbcTemplate jdbc;
    public FocusRecordDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<FocusRecord> MAPPER = (rs, n) -> {
        FocusRecord f = new FocusRecord();
        f.setId(rs.getLong("id"));
        f.setUserId(rs.getLong("user_id"));
        f.setRoomId(rs.getLong("room_id"));
        f.setDurationSeconds(rs.getInt("duration_seconds"));
        f.setMode(rs.getString("mode"));
        f.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
        f.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
        return f;
    };

    public Long insert(FocusRecord f) {
        String sql = "INSERT INTO focus_record(user_id, room_id, duration_seconds, mode, start_at, end_at) VALUES(?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, f.getUserId());
            ps.setLong(2, f.getRoomId());
            ps.setInt(3, f.getDurationSeconds());
            ps.setString(4, f.getMode());
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(f.getStartAt()));
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(f.getEndAt()));
            return ps;
        }, kh);
        return UserDao.extractId(kh);
    }

    public List<FocusRecord> listByUser(Long userId, int limit) {
        return jdbc.query("SELECT * FROM focus_record WHERE user_id = ? ORDER BY end_at DESC LIMIT ?",
                MAPPER, userId, limit);
    }

    /** 排行榜：按时间范围聚合专注时长 */
    public List<java.util.Map<String, Object>> rank(LocalDateTime from, LocalDateTime to, int limit) {
        String sql = "SELECT f.user_id AS userId, u.nickname AS nickname, u.avatar AS avatar, " +
                "u.class_id AS classId, SUM(f.duration_seconds) AS total " +
                "FROM focus_record f JOIN user u ON f.user_id = u.id " +
                "WHERE f.end_at >= ? AND f.end_at < ? " +
                "GROUP BY f.user_id, u.nickname, u.avatar, u.class_id " +
                "ORDER BY total DESC LIMIT ?";
        return jdbc.queryForList(sql, from, to, limit);
    }

    /** 趋势：按天聚合某用户专注时长 */
    public List<java.util.Map<String, Object>> trendByDay(Long userId, LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT CAST(end_at AS DATE) AS day, SUM(duration_seconds) AS total " +
                "FROM focus_record WHERE user_id = ? AND end_at >= ? AND end_at < ? " +
                "GROUP BY CAST(end_at AS DATE) ORDER BY day";
        return jdbc.queryForList(sql, userId, from, to);
    }
}
