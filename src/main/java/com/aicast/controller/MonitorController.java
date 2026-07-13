package com.aicast.controller;

import com.aicast.service.monitor.ResourceMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ResourceMonitorService resourceMonitorService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/resources")
    public ResponseEntity<List<Map<String, Object>>> getResources(
            @RequestParam(value = "limit", defaultValue = "60") int limit) {
        return ResponseEntity.ok(resourceMonitorService.getRecentResources(limit));
    }

    @GetMapping("/api-status")
    public ResponseEntity<List<Map<String, Object>>> getApiStatus() {
        // v_api_stat 뷰를 조회하여 최근 API 호출 현황 반환 (최근 1일)
        String sql = """
            SELECT gov_id, req_path, SUM(tot_cnt) as tot_cnt, 
                   SUM(ok_cnt) as ok_cnt, SUM(fail_cnt) as fail_cnt 
            FROM v_api_stat 
            WHERE call_dt = CURRENT_DATE 
            GROUP BY gov_id, req_path
        """;
        List<Map<String, Object>> statusList = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(statusList);
    }
}
