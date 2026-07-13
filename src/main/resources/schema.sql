-- API 호출 전체 로그 (tb_api_log)
CREATE TABLE IF NOT EXISTS tb_api_log (
    corr_id VARCHAR(36) PRIMARY KEY COMMENT 'Correlation ID (UUID)',
    api_key VARCHAR(100) NOT NULL COMMENT '호출자 API Key',
    gov_name VARCHAR(100) COMMENT '지자체명',
    endpoint VARCHAR(100) NOT NULL COMMENT '호출된 API 엔드포인트',
    client_ip VARCHAR(45) COMMENT '요청 클라이언트 IP',
    req_time DATETIME NOT NULL COMMENT '요청 수신 시간',
    res_time DATETIME COMMENT '응답 발송 시간',
    proc_ms INT COMMENT '총 처리 소요 시간 (ms)',
    is_ok BOOLEAN NOT NULL COMMENT '성공 여부',
    err_code VARCHAR(20) COMMENT '실패 시 에러 코드',
    err_msg TEXT COMMENT '실패 시 에러 메시지',
    step_logs JSON COMMENT '내부 처리 단계별 소요 시간 및 상태'
) COMMENT='API 호출 전체 상태 로그 (F-01, F-03 등)';

-- tb_api_log 인덱스
CREATE INDEX IF NOT EXISTS idx_api_log_time ON tb_api_log (req_time DESC);
CREATE INDEX IF NOT EXISTS idx_api_log_key ON tb_api_log (api_key);

-- AI 단위 서비스 호출 로그 (tb_ai_svc_log)
CREATE TABLE IF NOT EXISTS tb_ai_svc_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    corr_id VARCHAR(36) NOT NULL COMMENT '연관 Correlation ID',
    svc_type VARCHAR(20) NOT NULL COMMENT '서비스 타입 (STT, NLP, TRANSLATE, OCR, IMAGE_GEN)',
    api_key VARCHAR(100) NOT NULL COMMENT '호출자 API Key',
    req_time DATETIME NOT NULL COMMENT '호출 시작 시간',
    proc_ms INT COMMENT '처리 소요 시간 (ms)',
    is_ok BOOLEAN NOT NULL COMMENT '성공 여부',
    req_size INT COMMENT '요청 크기 (바이트/문자 수)',
    res_size INT COMMENT '응답 크기 (바이트/문자 수)',
    err_msg TEXT COMMENT '에러 메시지',
    FOREIGN KEY (corr_id) REFERENCES tb_api_log(corr_id) ON DELETE CASCADE
) COMMENT='AI 단위 서비스 호출 상세 로그 (F-09)';

-- tb_ai_svc_log 인덱스
CREATE INDEX IF NOT EXISTS idx_ai_svc_log_time ON tb_ai_svc_log (req_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_svc_log_type ON tb_ai_svc_log (svc_type);

-- AI 서비스 일별 통계 (tb_ai_svc_stat)
CREATE TABLE IF NOT EXISTS tb_ai_svc_stat (
    stat_dt DATE NOT NULL COMMENT '통계 일자',
    api_key VARCHAR(100) NOT NULL COMMENT 'API Key',
    gov_name VARCHAR(100) COMMENT '지자체명',
    svc_type VARCHAR(20) NOT NULL COMMENT '서비스 타입',
    tot_cnt INT NOT NULL DEFAULT 0 COMMENT '총 호출 수',
    ok_cnt INT NOT NULL DEFAULT 0 COMMENT '성공 호출 수',
    fail_cnt INT NOT NULL DEFAULT 0 COMMENT '실패 호출 수',
    avg_ms INT NOT NULL DEFAULT 0 COMMENT '평균 처리 시간 (ms)',
    PRIMARY KEY (stat_dt, api_key, svc_type)
) COMMENT='AI 서비스 일별 집계 통계 (F-11)';

-- 컨테이너 리소스 모니터링 로그 (tb_res_log)
CREATE TABLE IF NOT EXISTS tb_res_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chk_time DATETIME NOT NULL COMMENT '측정 시각',
    cpu_pct DECIMAL(5,2) COMMENT 'CPU 사용률 (%)',
    mem_mb INT COMMENT '메모리 사용량 (MB)',
    mem_lmt_mb INT COMMENT '메모리 한도 (MB)',
    net_rx BIGINT COMMENT '네트워크 수신 (bytes/sec)',
    net_tx BIGINT COMMENT '네트워크 송신 (bytes/sec)',
    disk_rd BIGINT COMMENT '디스크 읽기 (bytes/sec)',
    disk_wr BIGINT COMMENT '디스크 쓰기 (bytes/sec)'
) COMMENT='시스템 컨테이너 리소스 사용량 로그 (1시간 유지) (F-14)';

CREATE INDEX IF NOT EXISTS idx_res_log_time ON tb_res_log (chk_time DESC);

-- API 호출 통계 조회용 View (v_api_stat)
CREATE OR REPLACE VIEW v_api_stat AS
SELECT
    DATE(req_time) AS stat_dt,
    api_key,
    gov_name,
    COUNT(*) AS tot_req,
    SUM(CASE WHEN is_ok = 1 THEN 1 ELSE 0 END) AS ok_cnt,
    SUM(CASE WHEN is_ok = 0 THEN 1 ELSE 0 END) AS fail_cnt,
    ROUND(SUM(CASE WHEN is_ok = 1 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS ok_rate,
    ROUND(SUM(CASE WHEN is_ok = 0 THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) AS fail_rate
FROM tb_api_log
GROUP BY DATE(req_time), api_key, gov_name;
