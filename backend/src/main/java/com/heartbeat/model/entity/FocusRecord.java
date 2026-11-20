package com.heartbeat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FocusRecord {
    private Long id;
    private Long userId;
    private Long roomId;
    private Integer durationSeconds;
    private String mode;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
