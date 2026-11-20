package com.heartbeat.dao;

import com.heartbeat.model.entity.Room;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class RoomDao {

    private final JdbcTemplate jdbc;
    public RoomDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Room> MAPPER = (rs, n) -> {
        Room r = new Room();
        r.setId(rs.getLong("id"));
        r.setRoomNo(rs.getString("room_no"));
        r.setName(rs.getString("name"));
        r.setOwnerId(rs.getLong("owner_id"));
        r.setPasswordHash(rs.getString("password_hash"));
        r.setCapacity(rs.getInt("capacity"));
        r.setPomodoroMode(rs.getBoolean("pomodoro_mode"));
        r.setFocusMinutes(rs.getInt("focus_minutes"));
        r.setRestMinutes(rs.getInt("rest_minutes"));
        r.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    };

    public Long insert(Room r) {
        String sql = "INSERT INTO room(room_no, name, owner_id, password_hash, capacity, pomodoro_mode, focus_minutes, rest_minutes) VALUES(?,?,?,?,?,?,?,?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.getRoomNo());
            ps.setString(2, r.getName());
            ps.setLong(3, r.getOwnerId());
            if (r.getPasswordHash() == null) ps.setNull(4, java.sql.Types.VARCHAR); else ps.setString(4, r.getPasswordHash());
            ps.setInt(5, r.getCapacity());
            ps.setBoolean(6, Boolean.TRUE.equals(r.getPomodoroMode()));
            ps.setInt(7, r.getFocusMinutes());
            ps.setInt(8, r.getRestMinutes());
            return ps;
        }, kh);
        return UserDao.extractId(kh);
    }

    public Room findById(Long id) {
        List<Room> list = jdbc.query("SELECT * FROM room WHERE id = ?", MAPPER, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Room findByRoomNo(String roomNo) {
        List<Room> list = jdbc.query("SELECT * FROM room WHERE room_no = ?", MAPPER, roomNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Room> listAll() {
        return jdbc.query("SELECT * FROM room ORDER BY created_at DESC LIMIT 100", MAPPER);
    }

    public boolean existsRoomNo(String roomNo) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM room WHERE room_no = ?", Integer.class, roomNo);
        return n != null && n > 0;
    }

    /** 管理员删除房间：先清关联数据再删房间本体 */
    @org.springframework.transaction.annotation.Transactional
    public void deleteById(Long roomId) {
        jdbc.update("DELETE FROM checkin_diary WHERE record_id IN (SELECT id FROM focus_record WHERE room_id = ?)", roomId);
        jdbc.update("DELETE FROM focus_record WHERE room_id = ?", roomId);
        jdbc.update("DELETE FROM room_member WHERE room_id = ?", roomId);
        jdbc.update("DELETE FROM room WHERE id = ?", roomId);
    }
}
