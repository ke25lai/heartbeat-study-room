package com.heartbeat.manager;

import com.heartbeat.model.entity.User;
import com.heartbeat.model.enums.OnlineStatus;
import com.heartbeat.model.vo.MemberVO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间实时状态管理器 —— 内存中的"实时真相"。
 *
 * 并发设计要点（面试可讲）：
 * - 用 ConcurrentHashMap 而非 HashMap：WebSocket 的 onOpen/onMessage/onClose
 *   由 Tomcat 多个线程并发调用，普通 HashMap 会数据错乱。
 * - 成员内部用 ConcurrentHashMap<Long, MemberPresence> 支持高并发读写。
 * - 时长累加用 LongAdder（高并发计数优于 AtomicLong，减少 CAS 竞争）。
 */
@Component
public class RoomStateManager {

    /** roomId -> 房间实时态 */
    private final ConcurrentHashMap<Long, RoomRuntime> rooms = new ConcurrentHashMap<>();

    /** 房间实时态 */
    public static class RoomRuntime {
        private final Map<Long, MemberPresence> members = new ConcurrentHashMap<>();

        public Map<Long, MemberPresence> getMembers() { return members; }
    }

    /** 成员实时状态 */
    public static class MemberPresence {
        private final Long userId;
        private final String clientKey;
        private final String nickname;
        private final boolean anonymous;
        private final boolean owner;
        private volatile OnlineStatus status = OnlineStatus.IDLE;
        private final java.util.concurrent.atomic.LongAdder focusSeconds = new java.util.concurrent.atomic.LongAdder();
        private volatile long lastHeartbeat = System.currentTimeMillis();

        public MemberPresence(Long userId, String clientKey, String nickname, boolean anonymous, boolean owner) {
            this.userId = userId;
            this.clientKey = clientKey;
            this.nickname = nickname;
            this.anonymous = anonymous;
            this.owner = owner;
        }

        public Long getUserId() { return userId; }
        public String getClientKey() { return clientKey; }
        public String getDisplayName() { return anonymous ? "匿名者" : nickname; }
        public boolean isAnonymous() { return anonymous; }
        public boolean isOwner() { return owner; }
        public OnlineStatus getStatus() { return status; }
        public void setStatus(OnlineStatus status) { this.status = status; }
        public long getFocusSeconds() { return focusSeconds.sum(); }
        public void addFocusSeconds(long s) { focusSeconds.add(s); }
        public void resetFocusSeconds() { focusSeconds.reset(); }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public void touch() { this.lastHeartbeat = System.currentTimeMillis(); }
    }

    private RoomRuntime ensure(Long roomId) {
        return rooms.computeIfAbsent(roomId, k -> new RoomRuntime());
    }

    /** 加入房间（原子化，避免并发超限由 Service 层容量校验配合） */
    public void join(Long roomId, User user, String clientKey, boolean anonymous, boolean owner) {
        RoomRuntime rt = ensure(roomId);
        rt.getMembers().put(user.getId(),
                new MemberPresence(user.getId(), clientKey, user.getNickname(), anonymous, owner));
    }

    public void leave(Long roomId, Long userId) {
        RoomRuntime rt = rooms.get(roomId);
        if (rt != null) {
            rt.getMembers().remove(userId);
            if (rt.getMembers().isEmpty()) rooms.remove(roomId);
        }
    }

    public MemberPresence getMember(Long roomId, Long userId) {
        RoomRuntime rt = rooms.get(roomId);
        return rt == null ? null : rt.getMembers().get(userId);
    }

    public void setStatus(Long roomId, Long userId, OnlineStatus status) {
        MemberPresence m = getMember(roomId, userId);
        if (m != null) m.setStatus(status);
    }

    public void addFocusSeconds(Long roomId, Long userId, long seconds) {
        MemberPresence m = getMember(roomId, userId);
        if (m != null) m.addFocusSeconds(seconds);
    }

    public void resetFocusSeconds(Long roomId, Long userId) {
        MemberPresence m = getMember(roomId, userId);
        if (m != null) m.resetFocusSeconds();
    }

    public List<MemberVO> listMembers(Long roomId) {
        RoomRuntime rt = rooms.get(roomId);
        if (rt == null) return List.of();
        List<MemberVO> result = new ArrayList<>();
        for (MemberPresence m : rt.getMembers().values()) {
            MemberVO vo = new MemberVO();
            vo.setUserId(m.isAnonymous() ? null : m.getUserId());
            vo.setClientKey(m.getClientKey());
            vo.setNickname(m.getDisplayName());
            vo.setAnonymous(m.isAnonymous());
            vo.setStatus(m.getStatus());
            vo.setFocusSeconds(m.getFocusSeconds());
            vo.setOwner(m.isOwner());
            result.add(vo);
        }
        return result;
    }

    public int onlineCount(Long roomId) {
        RoomRuntime rt = rooms.get(roomId);
        return rt == null ? 0 : rt.getMembers().size();
    }

    /** 管理员删除房间时，清除内存实时态 */
    public void remove(Long roomId) {
        rooms.remove(roomId);
    }
}
