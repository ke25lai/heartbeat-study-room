package com.heartbeat.dao;

import com.heartbeat.model.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class UserDao {

    private final JdbcTemplate jdbc;

    public UserDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<User> MAPPER = (rs, n) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNickname(rs.getString("nickname"));
        u.setAvatar(rs.getString("avatar"));
        u.setClassId(rs.getObject("class_id") == null ? null : rs.getLong("class_id"));
        u.setTotalFocusSeconds(rs.getInt("total_focus_seconds"));
        String role = rs.getString("role");
        u.setRole(role == null ? "USER" : role);
        Object en = rs.getObject("enabled");
        u.setEnabled(en == null ? Boolean.TRUE : rs.getBoolean("enabled"));
        u.setCreatedAt(rs.getTimestamp("created_at") == null ? null
                : rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    };

    public Long insert(User u) {
        String sql = "INSERT INTO user(username, password_hash, nickname, avatar, class_id, role, enabled) VALUES(?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNickname());
            ps.setString(4, u.getAvatar());
            if (u.getClassId() == null) ps.setNull(5, java.sql.Types.BIGINT);
            else ps.setLong(5, u.getClassId());
            ps.setString(6, u.getRole() == null ? "USER" : u.getRole());
            ps.setBoolean(7, u.isEnabled());
            return ps;
        }, kh);
        return extractId(kh);
    }

    /**
     * 提取自增主键，兼容 H2 与 MySQL：
     * - H2 MySQL 模式返回列名为 "id"；
     * - MySQL Connector/J 8.x 返回列名为 "GENERATED_KEY"，且可能含多列。
     * 策略：优先取 id 列，否则取第一行的第一个非 null 值。
     */
    static Long extractId(KeyHolder kh) {
        if (kh.getKeyList() == null || kh.getKeyList().isEmpty()) {
            Number only = kh.getKey();
            return only == null ? null : only.longValue();
        }
        Map<String, Object> row = kh.getKeyList().get(0);
        Object id = row.get("id");
        if (id == null) {
            for (Object v : row.values()) {
                if (v != null) { id = v; break; }
            }
        }
        return id == null ? null : ((Number) id).longValue();
    }

    public User findByUsername(String username) {
        List<User> list = jdbc.query("SELECT * FROM user WHERE username = ?", MAPPER, username);
        return list.isEmpty() ? null : list.get(0);
    }

    public User findById(Long id) {
        List<User> list = jdbc.query("SELECT * FROM user WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsByUsername(String username) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, username);
        return n != null && n > 0;
    }

    /** 原子累加专注时长（避免并发读取-写入丢失更新） */
    public void addFocusSeconds(Long userId, int seconds) {
        jdbc.update("UPDATE user SET total_focus_seconds = total_focus_seconds + ? WHERE id = ?", seconds, userId);
    }

    // ===== 管理员相关 =====

    /** 用户列表（不含密码哈希）；逐列转成可安全序列化的基本类型 */
    public List<Map<String, Object>> listUsers(int limit) {
        String sql = "SELECT id, username, nickname, class_id, total_focus_seconds, role, enabled, created_at " +
                "FROM user ORDER BY id LIMIT ?";
        return jdbc.query(sql, (rs, n) -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", rs.getLong("id"));
            m.put("username", rs.getString("username"));
            m.put("nickname", rs.getString("nickname"));
            Object classId = rs.getObject("class_id");
            m.put("class_id", classId == null ? null : rs.getLong("class_id"));
            m.put("total_focus_seconds", rs.getInt("total_focus_seconds"));
            m.put("role", rs.getString("role"));
            m.put("enabled", rs.getBoolean("enabled"));
            java.sql.Timestamp ts = rs.getTimestamp("created_at");
            m.put("created_at", ts == null ? "" : ts.toLocalDateTime().toString());
            return m;
        }, limit);
    }

    public boolean setRole(Long userId, String role) {
        return jdbc.update("UPDATE user SET role = ? WHERE id = ?", role, userId) > 0;
    }

    public boolean setEnabled(Long userId, boolean enabled) {
        return jdbc.update("UPDATE user SET enabled = ? WHERE id = ?", enabled, userId) > 0;
    }

    public long countUsers() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM user", Long.class);
        return n == null ? 0 : n;
    }

    /** 平台统计：用户数、房间数、专注记录数与总时长 */
    public Map<String, Object> platformStats() {
        Long rooms = jdbc.queryForObject("SELECT COUNT(*) FROM room", Long.class);
        Map<String, Object> focus = jdbc.queryForObject(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(duration_seconds),0) AS sec FROM focus_record",
                (rs, n) -> java.util.Map.of("records", rs.getLong("cnt"), "seconds", rs.getLong("sec")));
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("users", countUsers());
        result.put("rooms", rooms == null ? 0 : rooms);
        result.put("focusRecords", focus == null ? 0 : focus.get("records"));
        result.put("totalFocusSeconds", focus == null ? 0 : focus.get("seconds"));
        return result;
    }
}
