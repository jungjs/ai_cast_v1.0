package com.aicast.service.monitor;

import com.aicast.domain.monitor.TbResLog;
import com.aicast.domain.monitor.TbResLogRepository;
import com.aicast.service.alert.SlackAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceMonitorService {

    private final TbResLogRepository resLogRepository;
    private final SlackAlertService slackAlertService;
    private final JdbcTemplate jdbcTemplate;

    private static final double CPU_CRITICAL_THRESHOLD = 90.0;
    private static final double MEM_CRITICAL_THRESHOLD = 90.0;

    /**
     * 5초마다 시스템 리소스를 수집하여 DB에 저장합니다. (F-14)
     */
    @Scheduled(fixedRate = 5000)
    public void collectResources() {
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);

            double cpuLoad = osBean.getCpuLoad() * 100;
            if (cpuLoad < 0) cpuLoad = 0; // 초기화 시 음수 방지

            long totalMem = osBean.getTotalMemorySize();
            long freeMem = osBean.getFreeMemorySize();
            long usedMem = totalMem - freeMem;

            double memUsagePct = ((double) usedMem / totalMem) * 100;

            String hostname = InetAddress.getLocalHost().getHostName();

            TbResLog resLog = TbResLog.builder()
                    .cpuPct(BigDecimal.valueOf(cpuLoad))
                    .memMb((int)(usedMem / (1024 * 1024)))
                    .memLmtMb((int)(totalMem / (1024 * 1024)))
                    .netRx(0L) // Mock 데이터 (OSHI 또는 실제 환경 지표 연동 필요)
                    .netTx(0L)
                    .diskRd(0L)
                    .diskWr(0L)
                    .build();

            resLogRepository.save(resLog);

            // 임계치 알림 로직 (F-16)
            if (cpuLoad > CPU_CRITICAL_THRESHOLD) {
                slackAlertService.sendAlert(
                    SlackAlertService.AlertLevel.CRITICAL,
                    String.format("[%s] CPU 사용률 위험: %.2f%%", hostname, cpuLoad),
                    "SYSTEM"
                );
            }
            if (memUsagePct > MEM_CRITICAL_THRESHOLD) {
                slackAlertService.sendAlert(
                    SlackAlertService.AlertLevel.CRITICAL,
                    String.format("[%s] 메모리 사용률 위험: %.2f%%", hostname, memUsagePct),
                    "SYSTEM"
                );
            }

        } catch (Exception e) {
            log.error("Failed to collect resources", e);
        }
    }

    /**
     * 1시간 초과된 오래된 데이터를 매시간 자동 삭제합니다. (F-15)
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanUpOldData() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        String sql = "DELETE FROM tb_res_log WHERE chk_time < ?";
        int deleted = jdbcTemplate.update(sql, threshold);
        log.info("Cleaned up {} old resource logs older than {}", deleted, threshold);
    }

    public List<Map<String, Object>> getRecentResources(int limit) {
        String sql = "SELECT * FROM tb_res_log ORDER BY chk_time DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }
}
