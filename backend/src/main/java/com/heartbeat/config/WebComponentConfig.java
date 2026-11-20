package com.heartbeat.config;

import com.heartbeat.filter.AdminFilter;
import com.heartbeat.filter.AuthFilter;
import com.heartbeat.filter.EncodingFilter;
import com.heartbeat.filter.RoomOwnerFilter;
import com.heartbeat.servlet.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 注册原生 Servlet、Filter、WebSocket Endpoint。
 *
 * 注意（踩坑记录）：Servlet/Filter 类上带 @Component 会被 Spring 按类名注册为 bean，
 * 这里的 @Bean 方法名若与之一致会触发 BeanDefinitionOverrideException。
 * 因此注册方法统一用 xxxRegistration 命名，并注入已存在的 bean 实例。
 */
@Configuration
public class WebComponentConfig {

    // ===== Servlet 注册 =====
    @Bean
    public ServletRegistrationBean<UserServlet> userServletRegistration(UserServlet s) {
        return new ServletRegistrationBean<>(s, "/api/user/*");
    }

    @Bean
    public ServletRegistrationBean<RoomServlet> roomServletRegistration(RoomServlet s) {
        return new ServletRegistrationBean<>(s, "/api/room/*");
    }

    @Bean
    public ServletRegistrationBean<RecordServlet> recordServletRegistration(RecordServlet s) {
        return new ServletRegistrationBean<>(s, "/api/record/*");
    }

    @Bean
    public ServletRegistrationBean<RankServlet> rankServletRegistration(RankServlet s) {
        return new ServletRegistrationBean<>(s, "/api/rank/*");
    }

    @Bean
    public ServletRegistrationBean<AdminServlet> adminServletRegistration(AdminServlet s) {
        return new ServletRegistrationBean<>(s, "/api/admin/*");
    }

    // ===== Filter 注册（order 越小越先执行）=====
    @Bean
    public FilterRegistrationBean<EncodingFilter> encodingFilterRegistration() {
        FilterRegistrationBean<EncodingFilter> b = new FilterRegistrationBean<>(new EncodingFilter());
        b.addUrlPatterns("/*");
        b.setOrder(1);
        return b;
    }

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter f) {
        FilterRegistrationBean<AuthFilter> b = new FilterRegistrationBean<>(f);
        b.addUrlPatterns("/api/*");
        b.setOrder(2);
        return b;
    }

    @Bean
    public FilterRegistrationBean<RoomOwnerFilter> roomOwnerFilterRegistration(RoomOwnerFilter f) {
        FilterRegistrationBean<RoomOwnerFilter> b = new FilterRegistrationBean<>(f);
        // 注意：Servlet url-pattern 的 * 只能出现在路径末尾，"/api/room/*/kick" 这类中间通配不生效。
        // 因此注册为 /api/room/*，由 Filter 内部按 URL 动作精确判断是否需要房主权限。
        b.addUrlPatterns("/api/room/*");
        b.setOrder(3);
        return b;
    }

    @Bean
    public FilterRegistrationBean<AdminFilter> adminFilterRegistration(AdminFilter f) {
        FilterRegistrationBean<AdminFilter> b = new FilterRegistrationBean<>(f);
        b.addUrlPatterns("/api/admin/*");
        b.setOrder(4);
        return b;
    }

    // ===== WebSocket Endpoint 自动注册（扫描 @ServerEndpoint）=====
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
