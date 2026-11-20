package com.heartbeat.config;

import com.heartbeat.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;

/**
 * 管理员模块启动初始化：
 * 1) 幂等补齐 user 表的 role / enabled 列（已存在的 MySQL 库靠 CREATE TABLE IF NOT EXISTS 不会自动加列）；
 * 2) 幂等创建默认管理员账号 admin / admin123。
 *
 * 用 INFORMATION_SCHEMA 判定列是否存在，MySQL 8 与 H2(MySQL 模式) 均兼容。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private final JdbcTemplate jdbc;

    public AdminInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("role", "VARCHAR(16) NOT NULL DEFAULT 'USER'");
        ensureColumn("enabled", "BOOLEAN NOT NULL DEFAULT TRUE");
        ensureAdmin();
    }

    private void ensureColumn(String column, String ddl) {
        try {
            // 用 JDBC 元数据判定，跨 MySQL/H2 通用；TABLE_NAME 传 null 由下面手动按表名精确匹配，
            // 避免 mysql 系统库的同名 user 表干扰。
            Boolean exists = jdbc.execute((Connection con) -> {
                String db = con.getCatalog(); // MySQL 下即当前库名，H2 下为库名
                try (ResultSet rs = con.getMetaData().getColumns(db, null, null, null)) {
                    while (rs.next()) {
                        String table = rs.getString("TABLE_NAME");
                        String col = rs.getString("COLUMN_NAME");
                        if ("user".equalsIgnoreCase(table) && column.equalsIgnoreCase(col)) {
                            return true;
                        }
                    }
                }
                return false;
            });
            if (!Boolean.TRUE.equals(exists)) {
                jdbc.execute("ALTER TABLE user ADD COLUMN " + column + " " + ddl);
                log.info("[admin] 已为 user 表补列: {}", column);
            }
        } catch (Exception e) {
            // 不阻断启动：H2/MySQL 权限或方言差异时仅告警，管理员功能可能受限
            log.warn("[admin] 检查/补列 {} 失败: {}", column, e.getMessage());
        }
    }

    private void ensureAdmin() {
        try {
            // 支持环境变量覆盖默认密码，避免公开仓库里出现固定后门账号
            String rawPassword = System.getenv("ADMIN_PASSWORD");
            if (rawPassword == null || rawPassword.isBlank()) rawPassword = "admin123";
            String hash = PasswordUtil.encode(rawPassword);
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE username = 'admin'", Integer.class);
            if (n != null && n > 0) {
                // 已存在则确保角色为 ADMIN、状态启用、密码为默认密码（本地演示项目，幂等重置避免历史遗留账号无法登录）
                jdbc.update("UPDATE user SET role = 'ADMIN', enabled = TRUE, password_hash = ? WHERE username = 'admin'", hash);
                log.info("[admin] 已重置默认管理员 admin 的角色与密码（密码来源：{}）",
                        System.getenv("ADMIN_PASSWORD") != null ? "环境变量 ADMIN_PASSWORD" : "内置默认值");
            } else {
                jdbc.update("INSERT INTO user(username, password_hash, nickname, role, enabled) VALUES(?,?,?,?,TRUE)",
                        "admin", hash, "超级管理员", "ADMIN");
                log.info("[admin] 已创建默认管理员 admin（请登录后尽快修改密码）");
            }
        } catch (Exception e) {
            log.warn("[admin] 初始化默认管理员失败: {}", e.getMessage());
        }
    }
}
