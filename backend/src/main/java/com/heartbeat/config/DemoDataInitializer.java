package com.heartbeat.config;

import com.heartbeat.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;

/**
 * 演示数据初始化器（仅 H2 profile 生效）。
 *
 * 目的：面试官 clone 仓库后，用 start-demo.bat 零配置启动，
 * 打开就能看到有内容的排行榜、个人主页趋势、房间列表，而不是空壳。
 *
 * 用 @Profile("h2") 严格隔离，绝不污染 MySQL 生产/开发库。
 * 演示账号统一密码 demo123，H2 为内存库，重启即自动重建。
 * @Order(HIGHEST_PRECEDENCE) 保证先于 AdminInitializer 插入，id 稳定从 1 开始。
 */
@Component
@Profile("h2")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private final JdbcTemplate jdbc;

    public DemoDataInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String hash = PasswordUtil.encode("demo123");

            // 演示用户：{用户名, 昵称, 班级id, 历史累计专注秒数, 当日专注分钟数}
            Object[][] users = {
                    {"demo_zhang", "小张不摆烂", 1, 18 * 3600 + 40 * 60, 120},
                    {"demo_alin",  "阿琳同学",   1, 15 * 3600 + 20 * 60, 95},
                    {"demo_ming",  "小明加油",   2, 12 * 3600 + 10 * 60, 80},
                    {"demo_lily",  "Lily学姐",   3, 9 * 3600 + 30 * 60,  70},
                    {"demo_wang",  "王同学",     2, 7 * 3600 + 5 * 60,   60},
                    {"demo_chen",  "晨晨",       3, 5 * 3600 + 45 * 60,  45}
            };

            Long[] userIds = new Long[users.length];
            for (int i = 0; i < users.length; i++) {
                Object[] u = users[i];
                jdbc.update("INSERT INTO user(username, password_hash, nickname, class_id, role, enabled, total_focus_seconds) "
                                + "VALUES(?,?,?,?, 'USER', TRUE, ?)",
                        u[0], hash, u[1], u[2], u[3]);
                userIds[i] = jdbc.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, u[0]);
            }

            // 演示房间（房主为前两个演示用户）
            jdbc.update("INSERT INTO room(room_no, name, owner_id, capacity, pomodoro_mode, focus_minutes, rest_minutes) "
                            + "VALUES(?,?,?,?,?,?,?)",
                    "DEMO0001", "考研冲刺自习室", userIds[0], 20, true, 25, 5);
            jdbc.update("INSERT INTO room(room_no, name, owner_id, capacity, pomodoro_mode, focus_minutes, rest_minutes) "
                            + "VALUES(?,?,?,?,?,?,?)",
                    "DEMO0002", "四六级备考室", userIds[1], 12, false, 30, 5);
            Long demoRoomId = jdbc.queryForObject("SELECT id FROM room WHERE room_no = ?", Long.class, "DEMO0001");

            // 演示专注记录：覆盖最近 7 天，其中今天有记录（日榜可见），本周内有记录（周榜可见）
            LocalDateTime now = LocalDateTime.now();

            for (int u = 0; u < users.length; u++) {
                int baseMinutes = (Integer) users[u][4];
                for (int d = 0; d < 7; d++) {
                    int minutes = baseMinutes - d * 3;
                    if (minutes < 15) minutes = 15;

                    LocalDate day = now.toLocalDate().minusDays(d);
                    LocalDateTime end = day.atTime(10 + (u % 6), 30);
                    if (end.isAfter(now)) end = now.minusMinutes(1);
                    LocalDateTime start = end.minusMinutes(minutes);

                    jdbc.update("INSERT INTO focus_record(user_id, room_id, duration_seconds, mode, start_at, end_at) "
                                    + "VALUES(?,?,?,?,?,?)",
                            userIds[u], demoRoomId, minutes * 60, d % 2 == 0 ? "POMODORO" : "FREE",
                            Timestamp.valueOf(start), Timestamp.valueOf(end));
                }
            }

            log.info("[demo] 演示数据已就绪：6 个用户 / 2 个房间 / 42 条专注记录（密码 demo123）");
        } catch (Exception e) {
            // H2 内存库每次全新启动，理论上不会冲突；失败仅告警，不阻断启动
            log.warn("[demo] 演示数据初始化失败: {}", e.getMessage());
        }
    }
}
