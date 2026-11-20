package com.heartbeat.servlet;

import com.heartbeat.dao.CheckinDiaryDao;
import com.heartbeat.dao.FocusRecordDao;
import com.heartbeat.model.dto.LoginRequest;
import com.heartbeat.model.dto.RegisterRequest;
import com.heartbeat.model.entity.User;
import com.heartbeat.service.FocusService;
import com.heartbeat.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/** 用户：注册、登录、退出、当前用户、日记、趋势 */
@Component
public class UserServlet extends BaseServlet {

    private final UserService userService;
    private final FocusService focusService;

    public UserServlet(UserService userService, FocusService focusService) {
        this.userService = userService;
        this.focusService = focusService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        switch (path) {
            case "/register" -> {
                try {
                    RegisterRequest body = readBody(req, RegisterRequest.class);
                    Long id = userService.register(body);
                    ok(resp, Map.of("id", id));
                } catch (IllegalStateException e) {
                    fail(resp, 409, e.getMessage());
                } catch (IllegalArgumentException e) {
                    fail(resp, 400, e.getMessage());
                }
            }
            case "/login" -> {
                LoginRequest body = readBody(req, LoginRequest.class);
                User user = userService.login(body);
                if (user == null) { fail(resp, 401, "用户名或密码错误"); return; }
                // 重新从库里取最新状态（含 role/enabled）
                user = userService.getById(user.getId());
                if (!user.isEnabled()) { fail(resp, 403, "账号已被封禁，无法登录"); return; }
                HttpSession session = req.getSession(true);
                session.setAttribute("user", user);
                ok(resp, Map.of("id", user.getId(), "username", user.getUsername(),
                        "nickname", user.getNickname(), "totalFocusSeconds", user.getTotalFocusSeconds(),
                        "role", user.getRole() == null ? "USER" : user.getRole()));
            }
            case "/logout" -> {
                HttpSession session = req.getSession(false);
                if (session != null) session.invalidate();
                ok(resp, null);
            }
            default -> fail(resp, 404, "未知接口");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";
        User me = currentUser(req);
        switch (path) {
            case "/me" -> ok(resp, Map.of("id", me.getId(), "username", me.getUsername(),
                    "nickname", me.getNickname(), "totalFocusSeconds", me.getTotalFocusSeconds(),
                    "role", me.getRole() == null ? "USER" : me.getRole()));
            case "/diary" -> ok(resp, Map.of("records", focusService.diary(me.getId(), 50)));
            case "/trend" -> ok(resp, Map.of("points", focusService.trendByDay(me.getId(), 14)));
            default -> fail(resp, 404, "未知接口");
        }
    }
}
