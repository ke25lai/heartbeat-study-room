package com.heartbeat.filter;

import jakarta.servlet.*;
import java.io.IOException;

/** 统一 UTF-8 编码，解决中文乱码 */
public class EncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        chain.doFilter(req, resp);
    }
}
