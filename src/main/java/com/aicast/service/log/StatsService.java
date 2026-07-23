package com.aicast.service.log;

import com.aicast.domain.log.TbAiSvcStat;
import com.aicast.domain.log.TbAiSvcStatRepository;
import com.aicast.dto.StatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final TbAiSvcStatRepository statRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 매일 00:10에 전일 로그 데이터를 집계하여 통계 테이블에 저장 (F-11)
     */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void aggregateDailyStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting daily stats aggregation for {}", yesterday);

        String sql = """
            INSERT INTO tb_ai_svc_stat 
                (stat_dt, api_key, gov_name, svc_type, tot_cnt, ok_cnt, fail_cnt, avg_ms,
                 tot_tokens, prompt_tokens, completion_tokens)
            SELECT 
                CAST(? AS DATE),
                l.api_key,
                g.name,
                l.svc_type,
                COUNT(*),
                COUNT(CASE WHEN l.is_ok = TRUE THEN 1 END),
                COUNT(CASE WHEN l.is_ok = FALSE THEN 1 END),
                COALESCE(AVG(l.proc_ms), 0),
                COALESCE(SUM(l.total_tokens), 0),
                COALESCE(SUM(l.prompt_tokens), 0),
                COALESCE(SUM(l.completion_tokens), 0)
            FROM tb_ai_svc_log l
            JOIN gov_list g ON l.api_key = g.api_key
            WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
            GROUP BY l.api_key, l.svc_type
        """;

        try {
            int insertedCount = jdbcTemplate.update(sql, yesterday.toString(), yesterday.toString());
            log.info("Daily stats aggregation completed. {} rows inserted.", insertedCount);
        } catch (Exception e) {
            log.error("Failed to aggregate daily stats for {}", yesterday, e);
        }
    }

    @Transactional
    public void aggregateTodayStats() {
        LocalDate today = LocalDate.now();
        log.info("Starting manual stats aggregation for today ({})", today);
        
        jdbcTemplate.update("DELETE FROM tb_ai_svc_stat WHERE stat_dt = ?", today.toString());

        String sql = """
            INSERT INTO tb_ai_svc_stat 
                (stat_dt, api_key, gov_name, svc_type, tot_cnt, ok_cnt, fail_cnt, avg_ms,
                 tot_tokens, prompt_tokens, completion_tokens)
            SELECT 
                CAST(? AS DATE),
                l.api_key,
                g.name,
                l.svc_type,
                COUNT(*),
                COUNT(CASE WHEN l.is_ok = TRUE THEN 1 END),
                COUNT(CASE WHEN l.is_ok = FALSE THEN 1 END),
                COALESCE(AVG(l.proc_ms), 0),
                COALESCE(SUM(l.total_tokens), 0),
                COALESCE(SUM(l.prompt_tokens), 0),
                COALESCE(SUM(l.completion_tokens), 0)
            FROM tb_ai_svc_log l
            JOIN gov_list g ON l.api_key = g.api_key
            WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
            GROUP BY l.api_key, l.svc_type
        """;

        try {
            int insertedCount = jdbcTemplate.update(sql, today.toString(), today.toString());
            log.info("Today's stats aggregation completed. {} rows inserted.", insertedCount);
        } catch (Exception e) {
            log.error("Failed to aggregate daily stats for {}", today, e);
        }
    }

    public StatsResponseDto getDailyStats(String govId, LocalDate date) {
        List<Map<String, Object>> aiStats = getDailyAiStats(govId, date);
        List<Map<String, Object>> apiStats = getApiCallStats(govId, date, date);
        List<Map<String, Object>> trendStats = getDailyTrendStats(govId, date);

        return StatsResponseDto.builder()
                .aiStats(aiStats)
                .apiStats(apiStats)
                .trendStats(trendStats)
                .build();
    }

    public StatsResponseDto getWeeklyStats(String govId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> aiStats = getMergedPeriodStats(govId, startDate, endDate);
        List<Map<String, Object>> apiStats = getApiCallStats(govId, startDate, endDate);
        List<Map<String, Object>> trendStats = getPeriodTrendStats(govId, startDate, endDate);

        return StatsResponseDto.builder()
                .aiStats(aiStats)
                .apiStats(apiStats)
                .trendStats(trendStats)
                .build();
    }

    public StatsResponseDto getMonthlyStats(String govId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> aiStats = getMergedPeriodStats(govId, startDate, endDate);
        List<Map<String, Object>> apiStats = getApiCallStats(govId, startDate, endDate);
        List<Map<String, Object>> trendStats = getPeriodTrendStats(govId, startDate, endDate);

        return StatsResponseDto.builder()
                .aiStats(aiStats)
                .apiStats(apiStats)
                .trendStats(trendStats)
                .build();
    }

    private List<Map<String, Object>> getDailyAiStats(String govId, LocalDate date) {
        boolean isToday = LocalDate.now().equals(date);

        if (isToday) {
            if ("ALL".equalsIgnoreCase(govId)) {
                String sql = """
                    SELECT CAST(? AS DATE) AS stat_dt, l.svc_type, 
                           COUNT(*) AS tot_cnt, 
                           SUM(CASE WHEN l.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                           SUM(CASE WHEN l.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt, 
                           COALESCE(AVG(l.proc_ms), 0) AS avg_ms, 
                           COALESCE(SUM(l.total_tokens), 0) AS tot_tokens, 
                           COALESCE(SUM(l.prompt_tokens), 0) AS prompt_tokens, 
                           COALESCE(SUM(l.completion_tokens), 0) AS completion_tokens
                    FROM tb_ai_svc_log l
                    WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                return jdbcTemplate.queryForList(sql, date.toString(), date.toString());
            } else {
                String sql = """
                    SELECT CAST(? AS DATE) AS stat_dt, l.svc_type, 
                           COUNT(*) AS tot_cnt, 
                           SUM(CASE WHEN l.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                           SUM(CASE WHEN l.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt, 
                           COALESCE(AVG(l.proc_ms), 0) AS avg_ms, 
                           COALESCE(SUM(l.total_tokens), 0) AS tot_tokens, 
                           COALESCE(SUM(l.prompt_tokens), 0) AS prompt_tokens, 
                           COALESCE(SUM(l.completion_tokens), 0) AS completion_tokens
                    FROM tb_ai_svc_log l
                    JOIN gov_list g ON l.api_key = g.api_key
                    WHERE g.id = ? AND CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                try {
                    Long longGovId = Long.valueOf(govId);
                    return jdbcTemplate.queryForList(sql, date.toString(), longGovId, date.toString());
                } catch (NumberFormatException e) {
                    log.error("Invalid govId format: {}", govId);
                    return List.of();
                }
            }
        }

        if ("ALL".equalsIgnoreCase(govId)) {
            String sql = """
                SELECT stat_dt, svc_type, 
                       SUM(tot_cnt) as tot_cnt, SUM(ok_cnt) as ok_cnt, SUM(fail_cnt) as fail_cnt, 
                       AVG(avg_ms) as avg_ms, SUM(tot_tokens) as tot_tokens, 
                       SUM(prompt_tokens) as prompt_tokens, SUM(completion_tokens) as completion_tokens
                FROM tb_ai_svc_stat
                WHERE stat_dt = ?
                GROUP BY svc_type
            """;
            return jdbcTemplate.queryForList(sql, date.toString());
        }

        String sql = """
            SELECT s.* 
            FROM tb_ai_svc_stat s
            JOIN gov_list g ON s.api_key = g.api_key
            WHERE g.id = ? AND s.stat_dt = ?
        """;
        try {
            Long longGovId = Long.valueOf(govId);
            return jdbcTemplate.queryForList(sql, longGovId, date.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid govId format: {}", govId);
            return List.of();
        }
    }

    /**
     * 지정한 기간의 통계를 집계하되, 오늘 날짜가 포함되어 있으면 오늘치 Raw 로그를 실시간 합산하여 반환
     */
    private List<Map<String, Object>> getMergedPeriodStats(String govId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        boolean includeToday = !today.isBefore(start) && !today.isAfter(end);

        java.util.Map<String, java.util.Map<String, Object>> merged = new java.util.HashMap<>();

        // 1. 오늘 이전까지의 기간에 대해 통계 테이블(tb_ai_svc_stat) 조회
        LocalDate endForStat = includeToday ? today.minusDays(1) : end;
        if (!endForStat.isBefore(start)) {
            List<Map<String, Object>> statData;
            if ("ALL".equalsIgnoreCase(govId)) {
                String sql = """
                    SELECT s.svc_type, SUM(s.tot_cnt) as tot_cnt, SUM(s.ok_cnt) as ok_cnt, SUM(s.fail_cnt) as fail_cnt,
                           SUM(s.tot_tokens) as tot_tokens, SUM(s.prompt_tokens) as prompt_tokens, SUM(s.completion_tokens) as completion_tokens
                    FROM tb_ai_svc_stat s
                    WHERE s.stat_dt BETWEEN ? AND ?
                    GROUP BY s.svc_type
                """;
                statData = jdbcTemplate.queryForList(sql, start.toString(), endForStat.toString());
            } else {
                String sql = """
                    SELECT s.svc_type, SUM(s.tot_cnt) as tot_cnt, SUM(s.ok_cnt) as ok_cnt, SUM(s.fail_cnt) as fail_cnt,
                           SUM(s.tot_tokens) as tot_tokens, SUM(s.prompt_tokens) as prompt_tokens, SUM(s.completion_tokens) as completion_tokens
                    FROM tb_ai_svc_stat s
                    JOIN gov_list g ON s.api_key = g.api_key
                    WHERE g.id = ? AND s.stat_dt BETWEEN ? AND ?
                    GROUP BY s.svc_type
                """;
                try {
                    Long longGovId = Long.valueOf(govId);
                    statData = jdbcTemplate.queryForList(sql, longGovId, start.toString(), endForStat.toString());
                } catch (NumberFormatException e) {
                    log.error("Invalid govId format: {}", govId);
                    statData = List.of();
                }
            }
            for (Map<String, Object> row : statData) {
                String svcType = (String) row.get("svc_type");
                merged.put(svcType, new java.util.HashMap<>(row));
            }
        }

        // 2. 오늘 날짜가 포함되어 있으면 오늘치 Raw 로그(tb_ai_svc_log) 실시간 집계 및 병합
        if (includeToday) {
            List<Map<String, Object>> todayRawData;
            if ("ALL".equalsIgnoreCase(govId)) {
                String sql = """
                    SELECT l.svc_type, 
                           COUNT(*) AS tot_cnt, 
                           SUM(CASE WHEN l.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                           SUM(CASE WHEN l.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt, 
                           COALESCE(SUM(l.total_tokens), 0) AS tot_tokens, 
                           COALESCE(SUM(l.prompt_tokens), 0) AS prompt_tokens, 
                           COALESCE(SUM(l.completion_tokens), 0) AS completion_tokens
                    FROM tb_ai_svc_log l
                    WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                todayRawData = jdbcTemplate.queryForList(sql, today.toString());
            } else {
                String sql = """
                    SELECT l.svc_type, 
                           COUNT(*) AS tot_cnt, 
                           SUM(CASE WHEN l.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                           SUM(CASE WHEN l.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt, 
                           COALESCE(SUM(l.total_tokens), 0) AS tot_tokens, 
                           COALESCE(SUM(l.prompt_tokens), 0) AS prompt_tokens, 
                           COALESCE(SUM(l.completion_tokens), 0) AS completion_tokens
                    FROM tb_ai_svc_log l
                    JOIN gov_list g ON l.api_key = g.api_key
                    WHERE g.id = ? AND CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                try {
                    Long longGovId = Long.valueOf(govId);
                    todayRawData = jdbcTemplate.queryForList(sql, longGovId, today.toString());
                } catch (NumberFormatException e) {
                    log.error("Invalid govId format: {}", govId);
                    todayRawData = List.of();
                }
            }

            for (Map<String, Object> row : todayRawData) {
                String svcType = (String) row.get("svc_type");
                java.util.Map<String, Object> existing = merged.get(svcType);
                if (existing == null) {
                    merged.put(svcType, new java.util.HashMap<>(row));
                } else {
                    existing.put("tot_cnt", getLongValue(existing, "tot_cnt") + getLongValue(row, "tot_cnt"));
                    existing.put("ok_cnt", getLongValue(existing, "ok_cnt") + getLongValue(row, "ok_cnt"));
                    existing.put("fail_cnt", getLongValue(existing, "fail_cnt") + getLongValue(row, "fail_cnt"));
                    existing.put("tot_tokens", getLongValue(existing, "tot_tokens") + getLongValue(row, "tot_tokens"));
                    existing.put("prompt_tokens", getLongValue(existing, "prompt_tokens") + getLongValue(row, "prompt_tokens"));
                    existing.put("completion_tokens", getLongValue(existing, "completion_tokens") + getLongValue(row, "completion_tokens"));
                }
            }
        }

        return new java.util.ArrayList<>(merged.values());
    }

    private List<Map<String, Object>> getApiCallStats(String govId, LocalDate start, LocalDate end) {
        if ("ALL".equalsIgnoreCase(govId)) {
            String sql = """
                SELECT 
                    al.endpoint, 
                    COUNT(*) AS tot_cnt, 
                    SUM(CASE WHEN al.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                    SUM(CASE WHEN al.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt
                FROM tb_api_log al
                WHERE CAST(al.req_time AS DATE) BETWEEN CAST(? AS DATE) AND CAST(? AS DATE)
                GROUP BY al.endpoint
            """;
            return jdbcTemplate.queryForList(sql, start.toString(), end.toString());
        } else {
            String sql = """
                SELECT 
                    al.endpoint, 
                    COUNT(*) AS tot_cnt, 
                    SUM(CASE WHEN al.is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt, 
                    SUM(CASE WHEN al.is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt
                FROM tb_api_log al
                JOIN gov_list g ON al.api_key = g.api_key
                WHERE g.id = ? AND CAST(al.req_time AS DATE) BETWEEN CAST(? AS DATE) AND CAST(? AS DATE)
                GROUP BY al.endpoint
            """;
            try {
                Long longGovId = Long.valueOf(govId);
                return jdbcTemplate.queryForList(sql, longGovId, start.toString(), end.toString());
            } catch (NumberFormatException e) {
                log.error("Invalid govId format: {}", govId);
                return List.of();
            }
        }
    }

    private List<Map<String, Object>> getDailyTrendStats(String govId, LocalDate date) {
        if ("ALL".equalsIgnoreCase(govId)) {
            String sql = """
                SELECT 
                    CONCAT(LPAD(HOUR(l.req_time), 2, '0'), '시') AS time_label, 
                    l.svc_type, 
                    COUNT(*) AS tot_cnt
                FROM tb_ai_svc_log l
                WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
                GROUP BY HOUR(l.req_time), l.svc_type
                ORDER BY HOUR(l.req_time) ASC
            """;
            return jdbcTemplate.queryForList(sql, date.toString());
        } else {
            String sql = """
                SELECT 
                    CONCAT(LPAD(HOUR(l.req_time), 2, '0'), '시') AS time_label, 
                    l.svc_type, 
                    COUNT(*) AS tot_cnt
                FROM tb_ai_svc_log l
                JOIN gov_list g ON l.api_key = g.api_key
                WHERE g.id = ? AND CAST(l.req_time AS DATE) = CAST(? AS DATE)
                GROUP BY HOUR(l.req_time), l.svc_type
                ORDER BY HOUR(l.req_time) ASC
            """;
            try {
                Long longGovId = Long.valueOf(govId);
                return jdbcTemplate.queryForList(sql, longGovId, date.toString());
            } catch (NumberFormatException e) {
                log.error("Invalid govId format: {}", govId);
                return List.of();
            }
        }
    }

    private List<Map<String, Object>> getPeriodTrendStats(String govId, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        boolean includeToday = !today.isBefore(start) && !today.isAfter(end);
        
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        LocalDate endForStat = includeToday ? today.minusDays(1) : end;
        if (!endForStat.isBefore(start)) {
            if ("ALL".equalsIgnoreCase(govId)) {
                String sql = """
                    SELECT 
                        CAST(s.stat_dt AS CHAR) AS time_label, 
                        s.svc_type, 
                        SUM(s.tot_cnt) AS tot_cnt
                    FROM tb_ai_svc_stat s
                    WHERE s.stat_dt BETWEEN ? AND ?
                    GROUP BY s.stat_dt, s.svc_type
                    ORDER BY s.stat_dt ASC
                """;
                result.addAll(jdbcTemplate.queryForList(sql, start.toString(), endForStat.toString()));
            } else {
                String sql = """
                    SELECT 
                        CAST(s.stat_dt AS CHAR) AS time_label, 
                        s.svc_type, 
                        SUM(s.tot_cnt) AS tot_cnt
                    FROM tb_ai_svc_stat s
                    JOIN gov_list g ON s.api_key = g.api_key
                    WHERE g.id = ? AND s.stat_dt BETWEEN ? AND ?
                    GROUP BY s.stat_dt, s.svc_type
                    ORDER BY s.stat_dt ASC
                """;
                try {
                    Long longGovId = Long.valueOf(govId);
                    result.addAll(jdbcTemplate.queryForList(sql, longGovId, start.toString(), endForStat.toString()));
                } catch (NumberFormatException e) {
                    log.error("Invalid govId format: {}", govId);
                }
            }
        }
        
        if (includeToday) {
            if ("ALL".equalsIgnoreCase(govId)) {
                String sql = """
                    SELECT 
                        CAST(? AS CHAR) AS time_label, 
                        l.svc_type, 
                        COUNT(*) AS tot_cnt
                    FROM tb_ai_svc_log l
                    WHERE CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                result.addAll(jdbcTemplate.queryForList(sql, today.toString(), today.toString()));
            } else {
                String sql = """
                    SELECT 
                        CAST(? AS CHAR) AS time_label, 
                        l.svc_type, 
                        COUNT(*) AS tot_cnt
                    FROM tb_ai_svc_log l
                    JOIN gov_list g ON l.api_key = g.api_key
                    WHERE g.id = ? AND CAST(l.req_time AS DATE) = CAST(? AS DATE)
                    GROUP BY l.svc_type
                """;
                try {
                    Long longGovId = Long.valueOf(govId);
                    result.addAll(jdbcTemplate.queryForList(sql, today.toString(), longGovId, today.toString()));
                } catch (NumberFormatException e) {
                    log.error("Invalid govId format: {}", govId);
                }
            }
        }
        
        return result;
    }

    private long getLongValue(Map<String, Object> map, String key) {
        if (map == null) return 0L;
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0L;
    }
}
