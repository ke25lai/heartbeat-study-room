package com.heartbeat.service;

import com.heartbeat.dao.RoomDao;
import com.heartbeat.dao.RoomMemberDao;
import com.heartbeat.manager.RoomStateManager;
import com.heartbeat.model.dto.CreateRoomRequest;
import com.heartbeat.model.dto.JoinRoomRequest;
import com.heartbeat.model.entity.Room;
import com.heartbeat.model.entity.User;
import com.heartbeat.util.PasswordUtil;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class RoomService {

    private final RoomDao roomDao;
    private final RoomMemberDao memberDao;
    private final RoomStateManager roomState;

    public RoomService(RoomDao roomDao, RoomMemberDao memberDao, RoomStateManager roomState) {
        this.roomDao = roomDao;
        this.memberDao = memberDao;
        this.roomState = roomState;
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    public Room create(User owner, CreateRoomRequest req) {
        Room r = new Room();
        r.setRoomNo(generateRoomNo());
        r.setName(req.getName() == null || req.getName().isBlank() ? "默认自习室" : req.getName().trim());
        r.setOwnerId(owner.getId());
        r.setPasswordHash(req.getPassword() == null || req.getPassword().isBlank()
                ? null : PasswordUtil.encode(req.getPassword()));
        r.setCapacity(req.getCapacity() == null || req.getCapacity() < 1 ? 10 : req.getCapacity());
        r.setPomodoroMode(Boolean.TRUE.equals(req.getPomodoroMode()));
        r.setFocusMinutes(req.getFocusMinutes() == null ? 25 : req.getFocusMinutes());
        r.setRestMinutes(req.getRestMinutes() == null ? 5 : req.getRestMinutes());
        Long id = roomDao.insert(r);
        r.setId(id);
        // 房主自动加入房间成员表
        memberDao.insert(id, owner.getId(), false);
        return r;
    }

    /** 加入房间，校验密码/封禁/人数；成功返回 Room */
    public Room join(User user, JoinRoomRequest req) {
        if (req.getRoomNo() == null || req.getRoomNo().isBlank()) {
            throw new IllegalArgumentException("请输入房间号");
        }
        Room room = roomDao.findByRoomNo(req.getRoomNo().trim());
        if (room == null) throw new IllegalStateException("房间不存在");

        if (memberDao.isBanned(room.getId(), user.getId())) {
            throw new IllegalStateException("你已被该房间封禁");
        }
        if (room.getPasswordHash() != null &&
                (req.getPassword() == null || !PasswordUtil.matches(req.getPassword(), room.getPasswordHash()))) {
            throw new IllegalStateException("房间密码错误");
        }
        boolean alreadyMember = memberDao.isMember(room.getId(), user.getId());
        if (!alreadyMember) {
            if (memberDao.countMembers(room.getId()) >= room.getCapacity()) {
                throw new IllegalStateException("房间人数已满");
            }
            memberDao.insert(room.getId(), user.getId(), Boolean.TRUE.equals(req.getAnonymous()));
        } else {
            memberDao.updateAnonymous(room.getId(), user.getId(), Boolean.TRUE.equals(req.getAnonymous()));
        }
        return room;
    }

    public Room getById(Long id) { return roomDao.findById(id); }

    /** 成员在房间内的匿名标记（以数据库为准） */
    public boolean isAnonymous(Long roomId, Long userId) { return memberDao.isAnonymous(roomId, userId); }

    public List<Room> list() { return roomDao.listAll(); }

    public boolean isOwner(Long roomId, Long userId) {
        Room r = roomDao.findById(roomId);
        return r != null && r.getOwnerId().equals(userId);
    }

    public void setBanned(Long roomId, Long userId, boolean banned) {
        memberDao.setBanned(roomId, userId, banned);
        if (banned) roomState.leave(roomId, userId);
    }

    /** 管理员删除房间及其关联数据 */
    public void deleteRoom(Long roomId) {
        roomDao.deleteById(roomId);
        roomState.remove(roomId);
    }

    /** 生成 6 位数字房间号 */
    private String generateRoomNo() {
        for (int i = 0; i < 10; i++) {
            String no = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!roomDao.existsRoomNo(no)) return no;
        }
        throw new IllegalStateException("房间号生成失败");
    }
}
