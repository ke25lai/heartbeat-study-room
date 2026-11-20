package com.heartbeat.model.dto;

import lombok.Data;

@Data
public class JoinRoomRequest {
    private String roomNo;
    private String password;
    private Boolean anonymous = false;
}
