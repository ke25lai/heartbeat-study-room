package com.heartbeat.servlet;

import com.heartbeat.model.entity.User;
import com.heartbeat.model.vo.ApiResponse;
import com.heartbeat.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 基类：封装 JSON 读写、统一响应、当前用户获取。
 *
 * 线程安全要点（面试可讲）：Servlet 是单例多线程，本类不定义任何可变实例字段，
 * 所有请求数据通过局部变量与 request/session 传递。
 */
public abstract class BaseServlet extends HttpServlet {

    protected <T> T readBody(HttpServletRequest req, Class<T> type) throws IOException {
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.fromJson(body.isEmpty() ? "{}" : body, type);
    }

    protected void writeJson(HttpServletResponse resp, ApiResponse<?> api) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(api.getCode());
        resp.getWriter().write(JsonUtil.toJson(api));
    }

    protected void ok(HttpServletResponse resp, Object data) throws IOException {
        writeJson(resp, ApiResponse.ok(data));
    }

    protected void fail(HttpServletResponse resp, int code, String msg) throws IOException {
        writeJson(resp, ApiResponse.error(code, msg));
    }

    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}
