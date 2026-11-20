package com.heartbeat.model.enums;

/** 房间在线状态 */
public enum OnlineStatus {
    IDLE("空闲"),
    STUDYING("学习中"),
    RESTING("休息中");

    private final String label;

    OnlineStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
