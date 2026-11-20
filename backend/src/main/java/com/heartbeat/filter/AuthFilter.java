package com.heartbeat.filter;

import com.heartbeat.model.entity.User;
import com.heartbeat.service.UserService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 登录态校验：白名单放行注册/登录，其余 /api/* 需登录；每次请求以数据库为准刷新用户状态 */
@Component
public class AuthFilter implements Filter {

    private final UserService userService;
    public AuthFilter(UserService userService) { this.userService = userService; }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String path = request.getRequestURI();

        // 白名单
        if (path.endsWith("/api/user/login") || path.endsWith("/api/user/register")) {
            chain.doFilter(req, resp);
            return;
        }

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) {
            reject(response, 401, "未登录");
            return;
        }

        // 以数据库为准刷新角色与启用状态：封禁/升降级即时生效，无需重新登录
        User latest = userService.getById(user.getId());
        if (latest == null) {
            reject(response, 401, "账号不存在");
            return;
        }
        if (!latest.isEnabled()) {
            reject(response, 403, "账号已被封禁");
            return;
        }
        // 把最新状态写回 session，供后续 AdminFilter / Servlet 使用
        session.setAttribute("user", latest);

        chain.doFilter(req, resp);
    }

    private void reject(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + msg + "\"}");
    }
}
