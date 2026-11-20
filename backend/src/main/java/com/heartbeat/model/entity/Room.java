package com.heartbeat.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Room {
    private Long id;
    private String roomNo;
    private String name;
    private Long ownerId;
    private String passwordHash;
    private Integer capacity;
    private Boolean pomodoroMode;
    private Integer focusMinutes;
    private Integer restMinutes;
    private LocalDateTime createdAt;
}
