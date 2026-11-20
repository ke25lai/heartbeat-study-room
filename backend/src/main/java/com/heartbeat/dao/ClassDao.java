package com.heartbeat.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClassDao {
    private final JdbcTemplate jdbc;
    public ClassDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<Long, String> nameMap() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name FROM class_group");
        Map<Long, String> map = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Number id = (Number) r.get("id");
            map.put(id.longValue(), String.valueOf(r.get("name")));
        }
        return map;
    }
}
