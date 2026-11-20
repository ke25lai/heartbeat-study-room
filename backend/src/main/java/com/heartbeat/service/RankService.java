package com.heartbeat.service;

import com.heartbeat.dao.ClassDao;
import com.heartbeat.dao.FocusRecordDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** 每日 / 每周排行榜，可按班级分组 */
@Service
public class RankService {

    private final FocusRecordDao recordDao;
    private final ClassDao classDao;
    private final RankCacheService rankCache;
    private final com.heartbeat.dao.UserDao userDao;

    @Value("${heartbeat.rank.group-by-class:false}")
    private boolean groupByClass;

    public RankService(FocusRecordDao recordDao, ClassDao classDao,
                       RankCacheService rankCache, com.heartbeat.dao.UserDao userDao) {
        this.recordDao = recordDao;
        this.classDao = classDao;
        this.rankCache = rankCache;
        this.userDao = userDao;
    }

    /** 每日榜 */
    public Map<String, Object> daily(Boolean groupOverride) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        return build(from, from.plusDays(1), groupOverride);
    }

    /** 每周榜（周一为起点） */
    public Map<String, Object> weekly(Boolean groupOverride) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime from = monday.atStartOfDay();
        return build(from, from.plusWeeks(1), groupOverride);
    }

    private Map<String, Object> build(LocalDateTime from, LocalDateTime to, Boolean groupOverride) {
        boolean group = groupOverride != null ? groupOverride : groupByClass;
        List<Map<String, Object>> rows = recordDao.rank(from, to, 200);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupByClass", group);

        if (!group) {
            // 优先从 Redis ZSet 取（带昵称补全与降级）
            List<Map<String, Object>> cached = fromCache(from);
            List<Map<String, Object>> list = new ArrayList<>();
            if (!cached.isEmpty()) {
                for (Map<String, Object> c : cached) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("rank", c.get("rank"));
                    item.put("totalSeconds", c.get("totalSeconds"));
                    Long uid = ((Number) c.get("userId")).longValue();
                    var u = userDao.findById(uid);
                    item.put("userId", uid);
                    item.put("nickname", u == null ? "用户" + uid : u.getNickname());
                    item.put("avatar", u == null ? "" : u.getAvatar());
                    list.add(item);
                }
            } else {
                for (int i = 0; i < rows.size(); i++) {
                    list.add(rankItem(rows.get(i), i + 1));
                }
            }
            result.put("list", list);
        } else {
            Map<Long, String> classNames = classDao.nameMap();
            Map<Object, List<Map<String, Object>>> grouped = rows.stream()
                    .collect(Collectors.groupingBy(r -> r.get("classId") == null ? 0L : ((Number) r.get("classId")).longValue(),
                            LinkedHashMap::new, Collectors.toList()));
            List<Map<String, Object>> groups = new ArrayList<>();
            for (Map.Entry<Object, List<Map<String, Object>>> e : grouped.entrySet()) {
                Long cid = (Long) e.getKey();
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("classId", cid == 0L ? null : cid);
                g.put("className", cid == 0L ? "未分组" : classNames.getOrDefault(cid, "班级" + cid));
                List<Map<String, Object>> list = new ArrayList<>();
                List<Map<String, Object>> items = e.getValue();
                for (int i = 0; i < items.size(); i++) list.add(rankItem(items.get(i), i + 1));
                g.put("list", list);
                groups.add(g);
            }
            result.put("groups", groups);
        }
        return result;
    }

    /** 根据查询起始时间判断取日榜还是周榜缓存 */
    private List<Map<String, Object>> fromCache(LocalDateTime from) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, LocalDateTime.now());
        return days <= 1 ? rankCache.topDaily(50) : rankCache.topWeekly(50);
    }

    private Map<String, Object> rankItem(Map<String, Object> row, int rank) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("rank", rank);
        item.put("userId", row.get("userId"));
        item.put("nickname", row.get("nickname"));
        item.put("avatar", row.get("avatar"));
        Object total = row.get("total");
        item.put("totalSeconds", total == null ? 0 : ((Number) total).longValue());
        return item;
    }
}
