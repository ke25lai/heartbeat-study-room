package com.heartbeat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 心跳在线自习室 - 启动类
 *
 * 注意：刻意使用原生 Servlet + WebSocket，而非 Spring MVC Controller，
 * 目的是掌握 Servlet 生命周期、线程模型与 HTTP 升级握手等底层原理。
 */
@SpringBootApplication
public class HeartBeatApplication {
    public static void main(String[] args) {
        SpringApplication.run(HeartBeatApplication.class, args);
    }
}
