package com.heartbeat.service;

import com.heartbeat.dao.UserDao;
import com.heartbeat.model.dto.LoginRequest;
import com.heartbeat.model.dto.RegisterRequest;
import com.heartbeat.model.entity.User;
import com.heartbeat.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserDao userDao;

    public UserService(UserDao userDao) { this.userDao = userDao; }

    /** 注册，返回新用户 id；用户名重复返回 null 由上层处理 */
    public Long register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        if (userDao.existsByUsername(req.getUsername())) {
            throw new IllegalStateException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.getUsername().trim());
        u.setPasswordHash(PasswordUtil.encode(req.getPassword()));
        u.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname());
        u.setAvatar("");
        u.setClassId(req.getClassId());
        return userDao.insert(u);
    }

    /** 登录校验，成功返回 User，失败返回 null */
    public User login(LoginRequest req) {
        if (req.getUsername() == null || req.getPassword() == null) return null;
        User u = userDao.findByUsername(req.getUsername().trim());
        if (u == null) return null;
        return PasswordUtil.matches(req.getPassword(), u.getPasswordHash()) ? u : null;
    }

    public User getById(Long id) { return userDao.findById(id); }
}
