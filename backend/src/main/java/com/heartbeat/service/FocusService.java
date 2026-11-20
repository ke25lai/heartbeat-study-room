package com.heartbeat.service;

import com.heartbeat.dao.CheckinDiaryDao;
import com.heartbeat.dao.FocusRecordDao;
import com.heartbeat.dao.UserDao;
import com.heartbeat.model.dto.FinishFocusRequest;
import com.heartbeat.model.entity.CheckinDiary;
import com.heartbeat.model.entity.FocusRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 专注记录、打卡日记、趋势 —— 事务边界在此 */
@Service
public class FocusService {

    private final FocusRecordDao recordDao;
    private final CheckinDiaryDao diaryDao;
    private final UserDao userDao;
    private final RankCacheService rankCache;

    public FocusService(FocusRecordDao recordDao, CheckinDiaryDao diaryDao,
                        UserDao userDao, RankCacheService rankCache) {
        this.recordDao = recordDao;
        this.diaryDao = diaryDao;
        this.userDao = userDao;
        this.rankCache = rankCache;
    }

    /**
     * 结束专注并落库（事务）。
     * 原子性保证：插入专注记录 + 累加用户总时长 + 写打卡日记 要么全成要么全败。
     */
    @Transactional
    public Long finish(Long userId, FinishFocusRequest req) {
        if (req.getDurationSeconds() == null || req.getDurationSeconds() <= 0) {
            throw new IllegalArgumentException("专注时长必须大于 0");
        }
        LocalDateTime end = LocalDateTime.now();
        int seconds = req.getDurationSeconds();

        FocusRecord f = new FocusRecord();
        f.setUserId(userId);
        f.setRoomId(req.getRoomId());
        f.setDurationSeconds(seconds);
        f.setMode(req.getMode() == null ? "FREE" : req.getMode());
        f.setStartAt(end.minusSeconds(seconds));
        f.setEndAt(end);
        Long recordId = recordDao.insert(f);

        userDao.addFocusSeconds(userId, seconds);
        // 同步更新 Redis ZSet 排行榜（不可用时会静默降级）
        rankCache.addFocus(userId, seconds);

        if (req.getDiaryContent() != null && !req.getDiaryContent().isBlank()) {
            CheckinDiary d = new CheckinDiary();
            d.setRecordId(recordId);
            d.setUserId(userId);
            d.setContent(req.getDiaryContent().trim());
            diaryDao.insert(d);
        }
        return recordId;
    }

    public List<Map<String, Object>> diary(Long userId, int limit) {
        return diaryDao.listWithDuration(userId, limit);
    }

    public List<Map<String, Object>> trendByDay(Long userId, int days) {
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        return recordDao.trendByDay(userId, to.minusDays(days), to);
    }
}
