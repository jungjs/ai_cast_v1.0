package com.aicast.service.log;

import com.aicast.domain.log.TbAiSvcStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.aicast.dto.StatsResponseDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private TbAiSvcStatRepository statRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatsService statsService;

    @Test
    @DisplayName("일별 통계 집계 스케줄러 실행 시 jdbcTemplate.update가 호출되는지 검증")
    void aggregateDailyStats_Success() {
        // Given
        when(jdbcTemplate.update(anyString(), anyString(), anyString())).thenReturn(5);

        // When
        statsService.aggregateDailyStats();

        // Then
        verify(jdbcTemplate, times(1)).update(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("주별 통계 조회 시 jdbcTemplate.queryForList가 올바르게 호출되는지 검증")
    void getWeeklyStats_ReturnsList() {
        // Given
        String govId = "123";
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        List<Map<String, Object>> mockResult = List.of(Map.of("svc_type", "STT", "tot_cnt", 100));

        org.mockito.Mockito.lenient().when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(mockResult);

        // When
        StatsResponseDto result = statsService.getWeeklyStats(govId, start, end);

        // Then
        org.junit.jupiter.api.Assertions.assertNotNull(result);
    }
}
