package com.heartbeat.model.enums;

/** 专注模式 */
public enum FocusMode {
    FREE("自由计时"), POMODORO("番茄钟");

    private final String label;

    FocusMode(String label) { this.label = label; }

    public String getLabel() { return label; }
}
