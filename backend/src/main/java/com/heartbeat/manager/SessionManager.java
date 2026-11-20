package com.heartbeat.manager;

import jakarta.websocket.Session;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * userId <-> WebSocket Session 映射，支持多端登录。
 * 通过它可向指定用户推送消息（踢人、通知）。
 */
@Component
public class SessionManager {

    /** userId -> 该用户的多个会话 */
    private final ConcurrentHashMap<Long, Set<Session>> userSessions = new ConcurrentHashMap<>();

    /** sessionId -> roomId，便于 onClose 时定位房间 */
    private final ConcurrentHashMap<String, Long> sessionRoom = new ConcurrentHashMap<>();

    /** roomId -> 房间内所有会话，用于广播 */
    private final ConcurrentHashMap<Long, Set<Session>> roomSessions = new ConcurrentHashMap<>();

    public void register(Long userId, Session session, Long roomId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionRoom.put(session.getId(), roomId);
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, Session session) {
        Set<Session> set = userSessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) userSessions.remove(userId);
        }
        Long roomId = sessionRoom.remove(session.getId());
        if (roomId != null) {
            Set<Session> rs = roomSessions.get(roomId);
            if (rs != null) {
                rs.remove(session);
                if (rs.isEmpty()) roomSessions.remove(roomId);
            }
        }
    }

    public Set<Session> roomSessions(Long roomId) {
        Set<Session> set = roomSessions.get(roomId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public Long roomOf(Session session) { return sessionRoom.get(session.getId()); }

    public Set<Session> sessionsOf(Long userId) {
        Set<Session> set = userSessions.get(userId);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public void sendToUser(Long userId, String message) {
        for (Session s : sessionsOf(userId)) {
            if (s.isOpen()) {
                synchronized (s) {
                    s.getAsyncRemote().sendText(message);
                }
            }
        }
    }

    /** 踢人：向该用户所有会话发消息并关闭连接 */
    public void kickUser(Long userId, String message) {
        for (Session s : sessionsOf(userId)) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendText(message);
                try { s.close(); } catch (Exception ignored) {}
            }
        }
        userSessions.remove(userId);
    }
}
