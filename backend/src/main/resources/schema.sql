-- 心跳在线自习室 建表语句（H2 MySQL 兼容模式 / MySQL 8.0 通用）

CREATE TABLE IF NOT EXISTS class_group (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    username            VARCHAR(32)  NOT NULL UNIQUE,
    password_hash       VARCHAR(100) NOT NULL,
    nickname            VARCHAR(32),
    avatar              VARCHAR(255),
    class_id            BIGINT,
    total_focus_seconds INT NOT NULL DEFAULT 0,
    role                VARCHAR(16) NOT NULL DEFAULT 'USER',
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_no        VARCHAR(12) NOT NULL UNIQUE,
    name           VARCHAR(64) NOT NULL,
    owner_id       BIGINT NOT NULL,
    password_hash  VARCHAR(100),
    capacity       INT NOT NULL DEFAULT 10,
    pomodoro_mode  BOOLEAN NOT NULL DEFAULT FALSE,
    focus_minutes  INT NOT NULL DEFAULT 25,
    rest_minutes   INT NOT NULL DEFAULT 5,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS room_member (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    anonymous  BOOLEAN NOT NULL DEFAULT FALSE,
    banned     BOOLEAN NOT NULL DEFAULT FALSE,
    joined_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_room_member UNIQUE (room_id, user_id)
);

CREATE TABLE IF NOT EXISTS focus_record (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL,
    room_id          BIGINT NOT NULL,
    duration_seconds INT NOT NULL,
    mode             VARCHAR(16) NOT NULL DEFAULT 'FREE',
    start_at         DATETIME NOT NULL,
    end_at           DATETIME NOT NULL,
    INDEX idx_focus_user_end (user_id, end_at),
    INDEX idx_focus_room_end (room_id, end_at),
    INDEX idx_record_end     (end_at)
);

CREATE TABLE IF NOT EXISTS checkin_diary (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id  BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    content    VARCHAR(200),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引已内联到 focus_record 建表语句中（ CREATE INDEX IF NOT EXISTS 在 MySQL 8 不支持）
