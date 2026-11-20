package com.heartbeat.service;

import com.heartbeat.manager.SessionManager;
import com.heartbeat.model.vo.WsMessage;
import com.heartbeat.util.JsonUtil;
import jakarta.websocket.Session;
import org.springframework.stereotype.Service;

import java.util.Set;

/** 通过 WebSocket 向房间广播 / 向个人推送通知 */
@Service
public class NotificationService {

    private final SessionManager sessionManager;

    public NotificationService(SessionManager sessionManager) { this.sessionManager = sessionManager; }

    /** 广播给房间内所有会话（按 roomId 过滤） */
    public void broadcastToRoom(Long roomId, Set<Session> sessions, WsMessage msg) {
        String json = JsonUtil.toJson(msg);
        for (Session s : sessions) {
            if (s.isOpen()) {
                synchronized (s) { s.getAsyncRemote().sendText(json); }
            }
        }
    }

    public void sendNoticeToUser(Long userId, String text, String level) {
        sessionManager.sendToUser(userId, JsonUtil.toJson(WsMessage.notice(text, level)));
    }
}
