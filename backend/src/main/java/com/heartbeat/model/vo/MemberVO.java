package com.heartbeat.model.vo;

import com.heartbeat.model.enums.OnlineStatus;
import lombok.Data;

/** 房间在线成员视图 */
@Data
public class MemberVO {
    private Long userId;
    /** 成员唯一标识：非匿名为 "u"+userId，匿名为 "a"+sessionId；前端据此增删，避免匿名者混淆 */
    private String clientKey;
    private String nickname;
    private Boolean anonymous;
    private OnlineStatus status;
    private Long focusSeconds;   // 本次已专注秒数
    private Boolean owner;
}
