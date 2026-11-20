package com.heartbeat.servlet;

import com.heartbeat.dao.UserDao;
import com.heartbeat.model.entity.Room;
import com.heartbeat.service.RoomService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** 管理后台：平台统计、用户管理、房间管理（受 AdminFilter 保护） */
@Component
public class AdminServlet extends BaseServlet {

    private final UserDao userDao;
    private final RoomService roomService;

    public AdminServlet(UserDao userDao, RoomService roomService) {
        this.userDao = userDao;
        this.roomService = roomService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        switch (path) {
            case "/stats" -> ok(resp, userDao.platformStats());
            case "/users" -> ok(resp, Map.of("users", userDao.listUsers(500)));
            case "/rooms" -> ok(resp, Map.of("rooms", roomService.list().stream().map(this::roomVO).toList()));
            default -> fail(resp, 404, "未知接口");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        try {
            switch (path) {
                case "/user/role" -> {
                    Map<String, Object> body = readBody(req, Map.class);
                    Long userId = ((Number) body.get("userId")).longValue();
                    String role = String.valueOf(body.get("role"));
                    if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                        fail(resp, 400, "角色只能是 USER 或 ADMIN");
                        return;
                    }
                    if (!userDao.setRole(userId, role)) { fail(resp, 404, "用户不存在"); return; }
                    ok(resp, null);
                }
                case "/user/enabled" -> {
                    Map<String, Object> body = readBody(req, Map.class);
                    Long userId = ((Number) body.get("userId")).longValue();
                    boolean enabled = Boolean.parseBoolean(String.valueOf(body.get("enabled")));
                    if (!userDao.setEnabled(userId, enabled)) { fail(resp, 404, "用户不存在"); return; }
                    ok(resp, null);
                }
                case "/room/delete" -> {
                    Map<String, Object> body = readBody(req, Map.class);
                    Long roomId = ((Number) body.get("roomId")).longValue();
                    roomService.deleteRoom(roomId);
                    ok(resp, null);
                }
                default -> fail(resp, 404, "未知接口");
            }
        } catch (IllegalArgumentException e) {
            fail(resp, 400, e.getMessage());
        }
    }

    private Map<String, Object> roomVO(Room r) {
        return Map.of(
                "id", r.getId(),
                "roomNo", r.getRoomNo(),
                "name", r.getName(),
                "ownerId", r.getOwnerId(),
                "capacity", r.getCapacity(),
                "pomodoroMode", r.getPomodoroMode(),
                "createdAt", r.getCreatedAt() == null ? "" : r.getCreatedAt().toString()
        );
    }
}
