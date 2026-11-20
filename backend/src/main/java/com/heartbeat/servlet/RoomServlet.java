package com.heartbeat.servlet;

import com.heartbeat.manager.RoomStateManager;
import com.heartbeat.manager.SessionManager;
import com.heartbeat.model.dto.CreateRoomRequest;
import com.heartbeat.model.dto.JoinRoomRequest;
import com.heartbeat.model.entity.Room;
import com.heartbeat.model.entity.User;
import com.heartbeat.service.RoomService;
import com.heartbeat.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Session;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** 房间：列表、创建、加入、详情、成员、离开、踢人、封禁 */
@Component
public class RoomServlet extends BaseServlet {

    private final RoomService roomService;
    private final RoomStateManager roomState;
    private final SessionManager sessionManager;

    public RoomServlet(RoomService roomService, RoomStateManager roomState, SessionManager sessionManager) {
        this.roomService = roomService;
        this.roomState = roomState;
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        User me = currentUser(req);
        try {
            switch (path) {
                case "/create" -> {
                    CreateRoomRequest body = readBody(req, CreateRoomRequest.class);
                    Room room = roomService.create(me, body);
                    ok(resp, roomVO(room));
                }
                case "/join" -> {
                    JoinRoomRequest body = readBody(req, JoinRoomRequest.class);
                    Room room = roomService.join(me, body);
                    ok(resp, roomVO(room));
                }
                case "/leave" -> {
                    // body 带 roomId
                    Map<String, Object> body = readBody(req, Map.class);
                    Long roomId = ((Number) body.get("roomId")).longValue();
                    roomState.leave(roomId, me.getId());
                    ok(resp, null);
                }
                default -> {
                    // 房主操作：/kick、/ban
                    Long roomId = parseRoomId(path);
                    String action = path.substring(path.lastIndexOf('/') + 1);
                    Map<String, Object> body = readBody(req, Map.class);
                    Long targetId = ((Number) body.get("targetUserId")).longValue();
                    if ("kick".equals(action)) {
                        Set<Session> sessions = sessionManager.sessionsOf(targetId);
                        sessionManager.kickUser(targetId, JsonUtil.toJson(
                                com.heartbeat.model.vo.WsMessage.system("KICKED").put("reason", "你被移出房间")));
                        roomState.leave(roomId, targetId);
                        ok(resp, null);
                    } else if ("ban".equals(action)) {
                        roomService.setBanned(roomId, targetId, true);
                        sessionManager.kickUser(targetId, JsonUtil.toJson(
                                com.heartbeat.model.vo.WsMessage.system("KICKED").put("reason", "你被封禁")));
                        ok(resp, null);
                    } else {
                        fail(resp, 404, "未知接口");
                    }
                }
            }
        } catch (IllegalStateException e) {
            fail(resp, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            fail(resp, 400, e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        if ("/list".equals(path)) {
            ok(resp, roomService.list().stream().map(this::roomVO).toList());
            return;
        }
        Long roomId = parseRoomId(path);
        if (roomId == null) { fail(resp, 404, "未知接口"); return; }
        if (path.endsWith("/members")) {
            ok(resp, roomState.listMembers(roomId));
        } else {
            Room r = roomService.getById(roomId);
            if (r == null) fail(resp, 404, "房间不存在"); else ok(resp, roomVO(r));
        }
    }

    private Long parseRoomId(String path) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/(\\d+)").matcher(path);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private Map<String, Object> roomVO(Room r) {
        return Map.of(
                "id", r.getId(),
                "roomNo", r.getRoomNo(),
                "name", r.getName(),
                "ownerId", r.getOwnerId(),
                "capacity", r.getCapacity(),
                "hasPassword", r.getPasswordHash() != null,
                "pomodoroMode", r.getPomodoroMode(),
                "focusMinutes", r.getFocusMinutes(),
                "restMinutes", r.getRestMinutes()
        );
    }
}
