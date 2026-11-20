package com.heartbeat;

import com.heartbeat.dao.*;
import com.heartbeat.model.dto.*;
import com.heartbeat.model.entity.*;
import com.heartbeat.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/** 核心闭环集成测试：注册 → 登录 → 建房间 → 加入 → 专注落库 → 排行榜 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoreFlowTest {

    @Autowired UserService userService;
    @Autowired RoomService roomService;
    @Autowired FocusService focusService;
    @Autowired RankService rankService;
    @Autowired UserDao userDao;
    @Autowired FocusRecordDao recordDao;

    static Long ownerId;
    static Long memberId;
    static Long roomId;
    static String roomNo;

    @Test @Order(1)
    void register() {
        RegisterRequest r1 = new RegisterRequest();
        r1.setUsername("owner_test"); r1.setPassword("123456"); r1.setNickname("房主");
        ownerId = userService.register(r1);
        assertNotNull(ownerId);

        RegisterRequest r2 = new RegisterRequest();
        r2.setUsername("member_test"); r2.setPassword("123456"); r2.setNickname("学友");
        memberId = userService.register(r2);
        assertNotNull(memberId);
    }

    @Test @Order(2)
    void login() {
        LoginRequest req = new LoginRequest();
        req.setUsername("owner_test"); req.setPassword("123456");
        User u = userService.login(req);
        assertNotNull(u);
        assertEquals(ownerId, u.getId());

        req.setPassword("wrong");
        assertNull(userService.login(req), "错误密码应登录失败");
    }

    @Test @Order(3)
    void createAndJoinRoom() {
        User owner = userDao.findById(ownerId);
        CreateRoomRequest cr = new CreateRoomRequest();
        cr.setName("测试自习室"); cr.setCapacity(5);
        Room room = roomService.create(owner, cr);
        assertNotNull(room.getId());
        roomId = room.getId();
        roomNo = room.getRoomNo();
        assertEquals(6, roomNo.length());

        User member = userDao.findById(memberId);
        JoinRoomRequest jr = new JoinRoomRequest();
        jr.setRoomNo(roomNo);
        Room joined = roomService.join(member, jr);
        assertEquals(roomId, joined.getId());
    }

    @Test @Order(4)
    void finishFocus() {
        FinishFocusRequest req = new FinishFocusRequest();
        req.setRoomId(roomId);
        req.setDurationSeconds(1500);   // 25 分钟
        req.setMode("POMODORO");
        req.setDiaryContent("今天很专注");
        Long recordId = focusService.finish(ownerId, req);
        assertNotNull(recordId);

        // 校验总时长已累加
        User owner = userDao.findById(ownerId);
        assertEquals(1500, owner.getTotalFocusSeconds());

        // 校验日记已写入
        var diary = focusService.diary(ownerId, 10);
        assertFalse(diary.isEmpty());
    }

    @Test @Order(5)
    void rank() {
        var daily = rankService.daily(false);
        assertNotNull(daily.get("list"));
        boolean found = ((java.util.List<?>) daily.get("list")).stream()
                .anyMatch(o -> ((java.util.Map<?, ?>) o).get("userId").equals(ownerId));
        assertTrue(found, "排行榜应包含完成专注的用户");
    }
}
