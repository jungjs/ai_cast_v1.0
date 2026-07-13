package com.aicast.service.monitor;

import com.aicast.domain.monitor.TbResLog;
import com.aicast.domain.monitor.TbResLogRepository;
import com.aicast.service.alert.SlackAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceMonitorServiceTest {

    @Mock
    private TbResLogRepository resLogRepository;

    @Mock
    private SlackAlertService slackAlertService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ResourceMonitorService resourceMonitorService;

    @Test
    @DisplayName("정상 리소스 상태에서 수집 및 DB 저장 검증 (슬랙 경고 미발송)")
    void collectResources_NormalState() {
        try (MockedStatic<ManagementFactory> mockedFactory = mockStatic(ManagementFactory.class)) {
            // Given
            com.sun.management.OperatingSystemMXBean mockOsBean = mock(com.sun.management.OperatingSystemMXBean.class);
            mockedFactory.when(() -> ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class))
                    .thenReturn(mockOsBean);

            when(mockOsBean.getCpuLoad()).thenReturn(0.50);
            when(mockOsBean.getTotalMemorySize()).thenReturn(1000L);
            when(mockOsBean.getFreeMemorySize()).thenReturn(500L);

            // When
            resourceMonitorService.collectResources();

            // Then
            verify(resLogRepository, times(1)).save(any(TbResLog.class));
            verifyNoInteractions(slackAlertService);
        }
    }

    @Test
    @DisplayName("CPU 사용량 90% 초과 시 슬랙 임계치 경고 발송 검증")
    void collectResources_CriticalCpuAlert() {
        try (MockedStatic<ManagementFactory> mockedFactory = mockStatic(ManagementFactory.class)) {
            // Given
            com.sun.management.OperatingSystemMXBean mockOsBean = mock(com.sun.management.OperatingSystemMXBean.class);
            mockedFactory.when(() -> ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class))
                    .thenReturn(mockOsBean);

            when(mockOsBean.getCpuLoad()).thenReturn(0.95);
            when(mockOsBean.getTotalMemorySize()).thenReturn(1000L);
            when(mockOsBean.getFreeMemorySize()).thenReturn(800L);

            // When
            resourceMonitorService.collectResources();

            // Then
            verify(resLogRepository, times(1)).save(any(TbResLog.class));
            verify(slackAlertService, times(1)).sendAlert(
                    eq(SlackAlertService.AlertLevel.CRITICAL),
                    contains("CPU 사용률 위험"),
                    eq("SYSTEM")
            );
        }
    }

    @Test
    @DisplayName("메모리 사용량 90% 초과 시 슬랙 임계치 경고 발송 검증")
    void collectResources_CriticalMemAlert() {
        try (MockedStatic<ManagementFactory> mockedFactory = mockStatic(ManagementFactory.class)) {
            // Given
            com.sun.management.OperatingSystemMXBean mockOsBean = mock(com.sun.management.OperatingSystemMXBean.class);
            mockedFactory.when(() -> ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class))
                    .thenReturn(mockOsBean);

            when(mockOsBean.getCpuLoad()).thenReturn(0.30);
            when(mockOsBean.getTotalMemorySize()).thenReturn(1000L);
            when(mockOsBean.getFreeMemorySize()).thenReturn(50L);

            // When
            resourceMonitorService.collectResources();

            // Then
            verify(resLogRepository, times(1)).save(any(TbResLog.class));
            verify(slackAlertService, times(1)).sendAlert(
                    eq(SlackAlertService.AlertLevel.CRITICAL),
                    contains("메모리 사용률 위험"),
                    eq("SYSTEM")
            );
        }
    }

    @Test
    @DisplayName("1시간 초과된 리소스 로그 삭제 검증")
    void cleanUpOldData_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), any(LocalDateTime.class))).thenReturn(5);

        // When
        resourceMonitorService.cleanUpOldData();

        // Then
        verify(jdbcTemplate, times(1)).update(
                eq("DELETE FROM tb_res_log WHERE chk_time < ?"),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("최근 수집 리소스 목록 조회 검증")
    void getRecentResources_Success() {
        // Given
        List<Map<String, Object>> mockList = Collections.singletonList(Collections.singletonMap("cpu_pct", 45.5));
        when(jdbcTemplate.queryForList(anyString(), anyInt())).thenReturn(mockList);

        // When
        List<Map<String, Object>> result = resourceMonitorService.getRecentResources(10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(45.5, result.get(0).get("cpu_pct"));
        verify(jdbcTemplate, times(1)).queryForList(
                eq("SELECT * FROM tb_res_log ORDER BY chk_time DESC LIMIT ?"),
                eq(10)
        );
    }
}
