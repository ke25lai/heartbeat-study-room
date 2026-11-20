package com.heartbeat.model.dto;

import lombok.Data;

@Data
public class FinishFocusRequest {
    private Long roomId;
    private Integer durationSeconds;
    private String mode;
    private String diaryContent;
}
