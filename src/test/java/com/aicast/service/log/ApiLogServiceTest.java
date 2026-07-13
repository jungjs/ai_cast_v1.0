package com.aicast.service.log;

import com.aicast.domain.log.TbApiLog;
import com.aicast.domain.log.TbApiLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiLogServiceTest {

    @Mock
    private TbApiLogRepository apiLogRepository;

    @InjectMocks
    private ApiLogService apiLogService;

    @Test
    @DisplayName("API 로그 저장 성공 검증")
    void saveLog_Success() {
        // Given
        TbApiLog log = TbApiLog.builder()
                .corrId("corr-123")
                .apiKey("apikey-456")
                .govName("test-gov")
                .endpoint("/api/v1/test")
                .clientIp("127.0.0.1")
                .reqTime(java.time.LocalDateTime.now())
                .isOk(true)
                .build();
        when(apiLogRepository.save(any(TbApiLog.class))).thenReturn(log);

        // When
        apiLogService.saveLog(log);

        // Then
        verify(apiLogRepository, times(1)).save(log);
    }

    @Test
    @DisplayName("API 로그 저장 실패 시 예외 복구 검증")
    void saveLog_ExceptionHandled() {
        // Given
        TbApiLog log = TbApiLog.builder().build();
        when(apiLogRepository.save(any(TbApiLog.class))).thenThrow(new RuntimeException("DB Connection Fail"));

        // When & Then - 비동기 에러 복구를 위해 예외가 밖으로 전파되지 않아야 함
        apiLogService.saveLog(log);

        verify(apiLogRepository, times(1)).save(log);
    }
}
