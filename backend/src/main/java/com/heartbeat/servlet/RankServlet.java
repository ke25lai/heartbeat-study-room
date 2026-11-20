package com.heartbeat.servlet;

import com.heartbeat.service.RankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 排行榜：每日 / 每周，可按班级分组 */
@Component
public class RankServlet extends BaseServlet {

    private final RankService rankService;
    public RankServlet(RankService rankService) { this.rankService = rankService; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        String groupParam = req.getParameter("groupByClass");
        Boolean group = groupParam == null ? null : Boolean.valueOf(groupParam);
        if ("/daily".equals(path)) {
            ok(resp, rankService.daily(group));
        } else if ("/weekly".equals(path)) {
            ok(resp, rankService.weekly(group));
        } else {
            fail(resp, 404, "未知接口");
        }
    }
}
