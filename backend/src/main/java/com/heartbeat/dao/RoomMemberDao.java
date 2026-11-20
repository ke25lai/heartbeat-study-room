package com.heartbeat.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoomMemberDao {

    private final JdbcTemplate jdbc;
    public RoomMemberDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(Long roomId, Long userId, boolean anonymous) {
        jdbc.update("INSERT INTO room_member(room_id, user_id, anonymous) VALUES(?,?,?)",
                roomId, userId, anonymous);
    }

    /** 更新匿名标记（重复加入时） */
    public void updateAnonymous(Long roomId, Long userId, boolean anonymous) {
        jdbc.update("UPDATE room_member SET anonymous = ? WHERE room_id = ? AND user_id = ?",
                anonymous, roomId, userId);
    }

    public boolean isMember(Long roomId, Long userId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_member WHERE room_id = ? AND user_id = ?",
                Integer.class, roomId, userId);
        return n != null && n > 0;
    }

    /** 读取成员加入房间时选择的匿名标记（WebSocket 握手时以数据库为准，客户端不可伪造） */
    public boolean isAnonymous(Long roomId, Long userId) {
        Boolean v = jdbc.queryForObject(
                "SELECT anonymous FROM room_member WHERE room_id = ? AND user_id = ?",
                Boolean.class, roomId, userId);
        return Boolean.TRUE.equals(v);
    }

    public boolean isBanned(Long roomId, Long userId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_member WHERE room_id = ? AND user_id = ? AND banned = TRUE",
                Integer.class, roomId, userId);
        return n != null && n > 0;
    }

    public int countMembers(Long roomId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM room_member WHERE room_id = ?", Integer.class, roomId);
        return n == null ? 0 : n;
    }

    public void setBanned(Long roomId, Long userId, boolean banned) {
        jdbc.update("UPDATE room_member SET banned = ? WHERE room_id = ? AND user_id = ?",
                banned, roomId, userId);
    }

    public void delete(Long roomId, Long userId) {
        jdbc.update("DELETE FROM room_member WHERE room_id = ? AND user_id = ?", roomId, userId);
    }

    public List<Long> listMemberUserIds(Long roomId) {
        return jdbc.queryForList("SELECT user_id FROM room_member WHERE room_id = ?", Long.class, roomId);
    }
}
