package com.heartbeat.servlet;

import com.heartbeat.model.dto.FinishFocusRequest;
import com.heartbeat.model.entity.User;
import com.heartbeat.service.FocusService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/** 专注记录：结束落库（HTTP 兜底）、列表 */
@Component
public class RecordServlet extends BaseServlet {

    private final FocusService focusService;
    public RecordServlet(FocusService focusService) { this.focusService = focusService; }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        User me = currentUser(req);
        if ("/finish".equals(path)) {
            try {
                FinishFocusRequest body = readBody(req, FinishFocusRequest.class);
                Long recordId = focusService.finish(me.getId(), body);
                ok(resp, Map.of("recordId", recordId));
            } catch (IllegalArgumentException e) {
                fail(resp, 400, e.getMessage());
            }
        } else {
            fail(resp, 404, "未知接口");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User me = currentUser(req);
        ok(resp, Map.of("records", focusService.diary(me.getId(), 50)));
    }
}
