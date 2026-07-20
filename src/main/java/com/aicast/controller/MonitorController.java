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
        // tb_api_log 테이블을 직접 실시간 조회하여 오늘 API 호출 현황 반환
        String sql = """
            SELECT gov_name, api_key, 
                   COUNT(*) AS tot_req, 
                   SUM(CASE WHEN is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                   SUM(CASE WHEN is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt 
            FROM tb_api_log 
            WHERE CAST(req_time AS DATE) = CURRENT_DATE 
            GROUP BY gov_name, api_key
        """;
        List<Map<String, Object>> statusList = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(statusList);
    }

    @GetMapping("/recent-logs")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs() {
        // tb_api_log 테이블에서 최근 10건의 API 호출 로그를 조회하여 반환
        String sql = """
            SELECT is_ok, req_time, endpoint, gov_name, proc_ms, corr_id 
            FROM tb_api_log 
            ORDER BY req_time DESC 
            LIMIT 10
        """;
        List<Map<String, Object>> logList = jdbcTemplate.queryForList(sql);
        return ResponseEntity.ok(logList);
    }
}
