package com.heartbeat.websocket;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 让原生 @ServerEndpoint（非 Spring 管理、每连接一个实例）能拿到 Spring Bean。
 * WebSocket 容器负责创建 Endpoint 实例，不走 Spring IoC，故用静态 ApplicationContext 桥接。
 */
@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ctx = applicationContext;
    }

    public static <T> T bean(Class<T> type) { return ctx.getBean(type); }
}
