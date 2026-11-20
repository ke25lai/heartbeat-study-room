package com.heartbeat.websocket;

import com.heartbeat.manager.RoomStateManager;
import com.heartbeat.manager.SessionManager;
import com.heartbeat.manager.TimerManager;
import com.heartbeat.model.dto.FinishFocusRequest;
import com.heartbeat.model.entity.Room;
import com.heartbeat.model.entity.User;
import com.heartbeat.model.enums.OnlineStatus;
import com.heartbeat.model.vo.WsMessage;
import com.heartbeat.service.FocusService;
import com.heartbeat.service.NotificationService;
import com.heartbeat.service.RoomService;
import com.heartbeat.util.JsonUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间实时频道：ws://host/ws/room/{roomId}
 *
 * 生命周期（面试可讲）：onOpen → onMessage/onError* → onClose。
 * Endpoint 默认每个连接一个实例，共享状态交给单例 Manager 管理。
 *
 * 匿名与身份（本次补齐）：
 * - 匿名标记以 room_member 表为准（加入时 HTTP 落库），握手时读取，客户端无法伪造；
 * - 每个成员用 clientKey 唯一标识：普通成员 "u"+userId，匿名成员 "a"+sessionId。
 *   所有广播只带 clientKey 与（匿名时脱敏的）昵称，不泄露匿名用户 id，
 *   前端据此增删成员，避免多个"匿名者"互相覆盖。
 */
@ServerEndpoint(value = "/ws/room/{roomId}", configurator = WsAuthConfigurator.class)
@Component
public class RoomEndpoint {

    // 通过静态桥接拿到 Spring Bean（Endpoint 非 Spring 管理）
    private static RoomStateManager state() { return SpringContext.bean(RoomStateManager.class); }
    private static SessionManager sessions() { return SpringContext.bean(SessionManager.class); }
    private static NotificationService notifier() { return SpringContext.bean(NotificationService.class); }
    private static FocusService focusService() { return SpringContext.bean(FocusService.class); }
    private static RoomService roomService() { return SpringContext.bean(RoomService.class); }
    private static TimerManager timer() { return SpringContext.bean(TimerManager.class); }

    /** roomId:userId -> 当前番茄钟专注段开始时间戳（毫秒），服务端权威时钟 */
    private static final ConcurrentHashMap<String, Long> focusStart = new ConcurrentHashMap<>();

    private User user;
    private Long roomId;
    private Session session;
    private boolean anonymous;
    private String clientKey;
    private String displayName;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config, @PathParam("roomId") Long roomId) {
        this.session = session;
        this.roomId = roomId;
        this.user = (User) config.getUserProperties().get("user");

        if (user == null) {
            closeWith("未登录");
            return;
        }
        Room room = roomService().getById(roomId);
        if (room == null) {
            closeWith("房间不存在");
            return;
        }
        boolean owner = room.getOwnerId().equals(user.getId());
        // 匿名以数据库成员记录为准，而非客户端声明
        this.anonymous = roomService().isAnonymous(roomId, user.getId());
        this.clientKey = (anonymous ? "a" + session.getId() : "u" + user.getId());
        this.displayName = anonymous ? "匿名者" : user.getNickname();

        state().join(roomId, user, clientKey, anonymous, owner);
        sessions().register(user.getId(), session, roomId);
        session.getUserProperties().put("userId", user.getId());

        // 推给新连接：当前成员快照 + 自己的 clientKey（前端据此识别"我"）
        send(session, WsMessage.system("MEMBER_SNAPSHOT")
                .put("members", state().listMembers(roomId))
                .put("selfKey", clientKey)
                .put("pomodoroMode", room.getPomodoroMode())
                .put("focusMinutes", room.getFocusMinutes())
                .put("restMinutes", room.getRestMinutes()));

        // 广播上线通知
        broadcast(WsMessage.system("MEMBER_ONLINE")
                .put("userId", anonymous ? null : user.getId())
                .put("clientKey", clientKey)
                .put("nickname", displayName));
        broadcast(WsMessage.notice(displayName + " 进入了自习室", "info"));
    }

    @OnMessage
    public void onMessage(String raw) {
        if (user == null) return;
        com.fasterxml.jackson.databind.JsonNode node;
        try { node = JsonUtil.mapper().readTree(raw); }
        catch (Exception e) { return; }
        String type = node.path("type").asText();

        switch (type) {
            case "HEARTBEAT" -> {
                com.heartbeat.manager.RoomStateManager.MemberPresence m = state().getMember(roomId, user.getId());
                if (m != null) m.touch();
            }
            case "STATUS_CHANGE" -> handleStatusChange(node.path("data").path("status").asText());
            case "FOCUS_FINISH" -> handleFinish(node.path("data"));
            default -> { /* 忽略未知类型 */ }
        }
    }

    /** 结算当前番茄钟专注段：把 focusStart 至今的秒数累加到成员的会话专注时长 */
    private void settleFocusSegment() {
        Long start = focusStart.remove(key());
        if (start == null) return;
        long seconds = Math.max(0, (System.currentTimeMillis() - start) / 1000);
        state().addFocusSeconds(roomId, user.getId(), seconds);
    }

    private void handleStatusChange(String statusText) {
        OnlineStatus status;
        try { status = OnlineStatus.valueOf(statusText); }
        catch (Exception e) { return; }

        state().setStatus(roomId, user.getId(), status);
        Room room = roomService().getById(roomId);
        boolean pomodoro = room != null && Boolean.TRUE.equals(room.getPomodoroMode());

        // 先取消上一阶段的定时任务（专注段/休息段）——重新进入状态时以最新状态为准
        timer().cancel(key() + ":pomo-focus");
        timer().cancel(key() + ":pomo-rest");

        if (status == OnlineStatus.STUDYING) {
            focusStart.put(key(), System.currentTimeMillis());
            if (pomodoro) scheduleFocusEnd(room);
        } else {
            // 离开专注（手动休息）：结算这一段
            settleFocusSegment();
            // 番茄钟模式下手动点休息也进入休息倒计时，到点自动切回专注
            if (pomodoro && status == OnlineStatus.RESTING) scheduleRestEnd(room);
        }

        broadcast(WsMessage.system("STATUS_CHANGE")
                .put("userId", anonymous ? null : user.getId())
                .put("clientKey", clientKey)
                .put("nickname", displayName)
                .put("status", status.name()));
        broadcast(WsMessage.notice(displayName + " " + status.getLabel(), "info"));
    }

    /** 专注段到点：结算时长 → 切休息 → 广播 → 排休息到点任务（形成循环） */
    private void scheduleFocusEnd(Room room) {
        timer().schedule(key() + ":pomo-focus", () -> {
            settleFocusSegment();
            state().setStatus(roomId, user.getId(), OnlineStatus.RESTING);
            broadcast(WsMessage.system("POMODORO_SWITCH")
                    .put("userId", anonymous ? null : user.getId())
                    .put("clientKey", clientKey)
                    .put("nickname", displayName)
                    .put("phase", "REST")
                    .put("durationSeconds", room.getRestMinutes() * 60L));
            broadcast(WsMessage.notice(displayName + " 完成一个番茄，进入休息", "info"));
            scheduleRestEnd(room);
        }, room.getFocusMinutes() * 60L);
    }

    /** 休息段到点：切回专注 → 广播 → 排下一个专注到点任务（闭环） */
    private void scheduleRestEnd(Room room) {
        timer().schedule(key() + ":pomo-rest", () -> {
            // 用户可能已离线或被结算，忽略不再房间的成员
            if (state().getMember(roomId, user.getId()) == null) return;
            state().setStatus(roomId, user.getId(), OnlineStatus.STUDYING);
            focusStart.put(key(), System.currentTimeMillis());
            broadcast(WsMessage.system("POMODORO_SWITCH")
                    .put("userId", anonymous ? null : user.getId())
                    .put("clientKey", clientKey)
                    .put("nickname", displayName)
                    .put("phase", "FOCUS")
                    .put("durationSeconds", room.getFocusMinutes() * 60L));
            broadcast(WsMessage.notice(displayName + " 休息结束，开始新的专注", "success"));
            scheduleFocusEnd(room);
        }, room.getRestMinutes() * 60L);
    }

    private void handleFinish(com.fasterxml.jackson.databind.JsonNode data) {
        // 结算当前专注段（若正在专注），得到本次会话累计专注秒数
        settleFocusSegment();
        int seconds = (int) state().getMember(roomId, user.getId()).getFocusSeconds();
        timer().cancel(key() + ":pomo-focus");
        timer().cancel(key() + ":pomo-rest");

        // 兜底：若客户端给了时长且服务端累计为 0（例如异常路径），采用客户端值
        if (seconds <= 0 && data.has("durationSeconds")) {
            seconds = Math.max(1, data.path("durationSeconds").asInt());
        }

        FinishFocusRequest req = new FinishFocusRequest();
        req.setRoomId(roomId);
        req.setDurationSeconds(seconds);
        req.setMode(data.path("mode").asText("FREE"));
        req.setDiaryContent(data.path("diaryContent").asText(null));

        focusService().finish(user.getId(), req);
        state().resetFocusSeconds(roomId, user.getId());
        state().setStatus(roomId, user.getId(), OnlineStatus.IDLE);

        broadcast(WsMessage.system("CHECKIN_DONE")
                .put("userId", anonymous ? null : user.getId())
                .put("clientKey", clientKey)
                .put("nickname", displayName)
                .put("durationSeconds", seconds));
        broadcast(WsMessage.notice(displayName + " 完成了一次 " + (seconds / 60) + " 分钟的专注", "success"));
    }

    @OnClose
    public void onClose() {
        if (user == null || roomId == null) return;
        settleFocusSegment();
        timer().cancel(key() + ":pomo-focus");
        timer().cancel(key() + ":pomo-rest");
        sessions().unregister(user.getId(), session);
        state().leave(roomId, user.getId());
        broadcast(WsMessage.system("MEMBER_OFFLINE")
                .put("userId", anonymous ? null : user.getId())
                .put("clientKey", clientKey));
        broadcast(WsMessage.notice(displayName + " 离开了自习室", "info"));
    }

    @OnError
    public void onError(Throwable t) {
        // 记录日志即可，避免连接异常影响其它用户
        System.err.println("[RoomEndpoint] 连接异常: " + t.getMessage());
    }

    private String key() { return TimerManager.key(roomId, user.getId()); }

    private void broadcast(WsMessage msg) {
        Set<Session> set = sessions().roomSessions(roomId);
        notifier().broadcastToRoom(roomId, set, msg);
    }

    private void send(Session s, Object obj) {
        if (s.isOpen()) {
            synchronized (s) { s.getAsyncRemote().sendText(JsonUtil.toJson(obj)); }
        }
    }

    private void closeWith(String reason) {
        try { session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason)); }
        catch (Exception ignored) {}
    }
}
