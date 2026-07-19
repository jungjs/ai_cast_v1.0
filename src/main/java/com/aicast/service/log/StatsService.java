package com.aicast.service.log;

import com.aicast.domain.log.TbAiSvcStat;
import com.aicast.domain.log.TbAiSvcStatRepository;
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

    public List<Map<String, Object>> getDailyStats(String govId, LocalDate date) {
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

    public List<Map<String, Object>> getWeeklyStats(String govId, LocalDate weekStart, LocalDate weekEnd) {
        if ("ALL".equalsIgnoreCase(govId)) {
            String sql = """
                SELECT s.svc_type, SUM(s.tot_cnt) as tot_cnt, SUM(s.ok_cnt) as ok_cnt, SUM(s.fail_cnt) as fail_cnt,
                       SUM(s.tot_tokens) as tot_tokens, SUM(s.prompt_tokens) as prompt_tokens, SUM(s.completion_tokens) as completion_tokens
                FROM tb_ai_svc_stat s
                WHERE s.stat_dt BETWEEN ? AND ?
                GROUP BY s.svc_type
            """;
            return jdbcTemplate.queryForList(sql, weekStart.toString(), weekEnd.toString());
        }

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
            return jdbcTemplate.queryForList(sql, longGovId, weekStart.toString(), weekEnd.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid govId format: {}", govId);
            return List.of();
        }
    }

    public List<Map<String, Object>> getMonthlyStats(String govId, LocalDate monthStart, LocalDate monthEnd) {
        if ("ALL".equalsIgnoreCase(govId)) {
            String sql = """
                SELECT s.svc_type, SUM(s.tot_cnt) as tot_cnt, SUM(s.ok_cnt) as ok_cnt, SUM(s.fail_cnt) as fail_cnt,
                       SUM(s.tot_tokens) as tot_tokens, SUM(s.prompt_tokens) as prompt_tokens, SUM(s.completion_tokens) as completion_tokens
                FROM tb_ai_svc_stat s
                WHERE s.stat_dt BETWEEN ? AND ?
                GROUP BY s.svc_type
            """;
            return jdbcTemplate.queryForList(sql, monthStart.toString(), monthEnd.toString());
        }

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
            return jdbcTemplate.queryForList(sql, longGovId, monthStart.toString(), monthEnd.toString());
        } catch (NumberFormatException e) {
            log.error("Invalid govId format: {}", govId);
            return List.of();
        }
    }
}
