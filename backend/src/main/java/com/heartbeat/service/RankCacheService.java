package com.heartbeat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis ZSet 排行榜缓存层。
 *
 * 为什么用 ZSet（面试可讲）：
 * - 底层是跳表 + 哈希表：ZINCRBY O(logN)，ZREVRANGE 取 TopN 高效；
 * - 分数即专注秒数，天然有序，免去 SQL 的 GROUP BY + ORDER BY 聚合。
 * - 降级策略：Redis 关闭或异常时，RankService 回退到数据库聚合。
 */
@Service
public class RankCacheService {

    private final StringRedisTemplate redis;

    @Value("${heartbeat.redis.enabled:true}")
    private boolean enabled;

    public RankCacheService(StringRedisTemplate redis) { this.redis = redis; }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("yyyy-'W'ww");

    private String dailyKey() { return "rank:daily:" + LocalDate.now().format(DAY); }
    private String weeklyKey() { return "rank:weekly:" + LocalDate.now().format(WEEK); }

    /** 专注完成后累加分数到日榜与周榜 */
    public void addFocus(Long userId, int seconds) {
        if (!enabled) return;
        try {
            String member = String.valueOf(userId);
            redis.opsForZSet().incrementScore(dailyKey(), member, seconds);
            redis.opsForZSet().incrementScore(weeklyKey(), member, seconds);
        } catch (Exception e) {
            // Redis 不可用：静默降级，数据库仍是权威数据源
        }
    }

    /** 从缓存取 TopN；返回空表示未启用/不可用，触发回退 */
    public List<Map<String, Object>> topDaily(int n) { return top(dailyKey(), n); }
    public List<Map<String, Object>> topWeekly(int n) { return top(weeklyKey(), n); }

    private List<Map<String, Object>> top(String key, int n) {
        if (!enabled) return List.of();
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redis.opsForZSet().reverseRangeWithScores(key, 0, n - 1);
            if (tuples == null || tuples.isEmpty()) return List.of();
            List<Map<String, Object>> result = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> t : tuples) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("userId", Long.valueOf(t.getValue()));
                item.put("totalSeconds", t.getScore() == null ? 0 : t.getScore().longValue());
                item.put("rank", rank++);
                result.add(item);
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
}
