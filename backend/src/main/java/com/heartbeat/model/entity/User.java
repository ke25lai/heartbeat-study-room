package com.heartbeat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private Long classId;
    private Integer totalFocusSeconds;
    /** 角色：USER 普通用户 / ADMIN 管理员 */
    private String role;
    /** 是否被管理员全站封禁 */
    private Boolean enabled;
    private LocalDateTime createdAt;

    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isEnabled() { return !Boolean.FALSE.equals(enabled); }
}
