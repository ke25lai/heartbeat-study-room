package com.heartbeat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckinDiary {
    private Long id;
    private Long recordId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
