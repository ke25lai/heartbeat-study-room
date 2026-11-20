package com.heartbeat.model.dto;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private String name;
    private String password;
    private Integer capacity = 10;
    private Boolean pomodoroMode = false;
    private Integer focusMinutes = 25;
    private Integer restMinutes = 5;
}
