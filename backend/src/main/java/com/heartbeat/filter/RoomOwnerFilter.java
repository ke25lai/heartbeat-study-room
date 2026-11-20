package com.heartbeat.filter;

import com.heartbeat.model.entity.User;
import com.heartbeat.service.RoomService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 房主权限校验：仅房主可执行踢人/封禁/设置 */
@Component
public class RoomOwnerFilter implements Filter {

    private final RoomService roomService;
    public RoomOwnerFilter(RoomService roomService) { this.roomService = roomService; }

    private static final Pattern ROOM_ID = Pattern.compile("/api/room/(\\d+)/");

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String uri = request.getRequestURI();
        // 仅房主专属动作需要校验；房间列表/创建/加入/详情/成员列表等放行
        boolean ownerAction = uri.endsWith("/kick") || uri.endsWith("/ban") || uri.endsWith("/settings");
        if (!ownerAction) {
            chain.doFilter(req, resp);
            return;
        }

        Matcher m = ROOM_ID.matcher(uri);
        Long roomId = m.find() ? Long.valueOf(m.group(1)) : null;
        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (roomId == null || user == null || !roomService.isOwner(roomId, user.getId())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"仅房主可操作\"}");
            return;
        }
        chain.doFilter(req, resp);
    }
}
