package com.aicast.client;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbSchemaDebugTest {

    @Test
    void forceAggregateToday() throws Exception {
        String url = "jdbc:mariadb://211.248.161.43:43306/ai_smartcast";
        String user = "ai_aventusm";
        String password = "ai_aventusm!!";
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            // 오늘 통계 삭제 후 집계 쿼리 실행
            stmt.executeUpdate("DELETE FROM tb_ai_svc_stat WHERE stat_dt = CURDATE()");
            
            String sql = """
                INSERT INTO tb_ai_svc_stat 
                    (stat_dt, api_key, gov_name, svc_type, tot_cnt, ok_cnt, fail_cnt, avg_ms,
                     tot_tokens, prompt_tokens, completion_tokens)
                SELECT 
                    CURDATE(),
                    l.api_key,
                    g.api_key, -- api_key를 gov_name 컬럼에 임시 매핑하거나 또는 지자체 조인 테이블 활용
                    l.svc_type,
                    COUNT(*),
                    COUNT(CASE WHEN l.is_ok = TRUE THEN 1 END),
                    COUNT(CASE WHEN l.is_ok = FALSE THEN 1 END),
                    COALESCE(AVG(l.proc_ms), 0),
                    COALESCE(SUM(l.total_tokens), 0),
                    COALESCE(SUM(l.prompt_tokens), 0),
                    COALESCE(SUM(l.completion_tokens), 0)
                FROM tb_ai_svc_log l
                GROUP BY l.api_key, l.svc_type
            """;
            
            // 지자체명 조인 테이블(gov_list)을 활용한 정석 쿼리도 같이 확인
            String realSql = """
                INSERT INTO tb_ai_svc_stat 
                    (stat_dt, api_key, gov_name, svc_type, tot_cnt, ok_cnt, fail_cnt, avg_ms,
                     tot_tokens, prompt_tokens, completion_tokens)
                SELECT 
                    CURDATE(),
                    l.api_key,
                    g.name, -- g.name이 맞습니다
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
                GROUP BY l.api_key, l.svc_type
            """;

            int inserted = 0;
            try {
                inserted = stmt.executeUpdate(realSql);
            } catch (Exception e) {
                System.out.println("Failed with realSql, error: " + e.getMessage());
                System.out.println("Falling back to basic aggregation query...");
                inserted = stmt.executeUpdate(sql);
            }
            
            System.out.println("Force aggregation complete. Inserted stats rows: " + inserted);
            
            // tb_ai_svc_stat 건수 출력
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tb_ai_svc_stat")) {
                if (rs.next()) System.out.println("Updated tb_ai_svc_stat Count: " + rs.getInt(1));
            }
        }
    }
}
