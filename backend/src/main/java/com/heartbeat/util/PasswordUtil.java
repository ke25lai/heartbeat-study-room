package com.heartbeat.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** BCrypt 加盐哈希：抵御彩虹表，每次哈希盐不同 */
public final class PasswordUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {}

    public static String encode(String raw) { return ENCODER.encode(raw); }

    public static boolean matches(String raw, String hash) { return ENCODER.matches(raw, hash); }
}
