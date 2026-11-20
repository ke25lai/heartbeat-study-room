package com.heartbeat.websocket;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * WebSocket 握手拦截器：把 HttpSession 中的登录用户透传到 Endpoint。
 *
 * 底层原理（面试可讲）：
 * WebSocket 握手本质是一次 HTTP 请求。Tomcat 在 HTTP Upgrade 阶段调用 Configurator，
 * 此时还能拿到 HttpSession；升级成 WebSocket 连接后就没有 Servlet API 了，
 * 所以必须在这个时机把身份信息存入 userProperties。
 */
public class WsAuthConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        Object session = request.getHttpSession();
        if (session instanceof HttpSession httpSession) {
            Object user = httpSession.getAttribute("user");
            if (user != null) sec.getUserProperties().put("user", user);
        }
    }
}
