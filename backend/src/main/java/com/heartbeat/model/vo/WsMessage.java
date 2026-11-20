package com.heartbeat.model.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** WebSocket 统一消息格式 */
@Data
public class WsMessage {
    private String type;
    private String from;
    private String fromName;
    private long timestamp;
    private Map<String, Object> data = new LinkedHashMap<>();

    public static WsMessage of(String type, String from, String fromName) {
        WsMessage m = new WsMessage();
        m.setType(type);
        m.setFrom(from);
        m.setFromName(fromName);
        m.setTimestamp(System.currentTimeMillis());
        return m;
    }

    public WsMessage put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public static WsMessage notice(String text, String level) {
        return of("NOTICE", "SYSTEM", "系统")
                .put("text", text).put("level", level == null ? "info" : level);
    }

    public static WsMessage system(String type) {
        return of(type, "SYSTEM", "系统");
    }
}
