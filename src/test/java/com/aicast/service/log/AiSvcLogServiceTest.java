package com.aicast.service.log;

import com.aicast.domain.log.TbAiSvcLog;
import com.aicast.domain.log.TbAiSvcLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiSvcLogServiceTest {

    @Mock
    private TbAiSvcLogRepository aiSvcLogRepository;

    @InjectMocks
    private AiSvcLogService aiSvcLogService;

    @Test
    @DisplayName("AI 서비스 로그 저장 성공 검증")
    void saveLog_Success() {
        // Given
        TbAiSvcLog log = TbAiSvcLog.builder()
                .corrId("corr-123")
                .apiKey("apikey-456")
                .svcType("STT")
                .procMs(120)
                .isOk(true)
                .build();
        when(aiSvcLogRepository.save(any(TbAiSvcLog.class))).thenReturn(log);

        // When
        aiSvcLogService.saveLog(log);

        // Then
        verify(aiSvcLogRepository, times(1)).save(log);
    }

    @Test
    @DisplayName("AI 서비스 로그 저장 실패 시 예외 복구 검증")
    void saveLog_ExceptionHandled() {
        // Given
        TbAiSvcLog log = TbAiSvcLog.builder().build();
        when(aiSvcLogRepository.save(any(TbAiSvcLog.class))).thenThrow(new RuntimeException("DB Connection Fail"));

        // When & Then - 비동기 에러 복구를 위해 예외가 밖으로 전파되지 않아야 함
        aiSvcLogService.saveLog(log);

        verify(aiSvcLogRepository, times(1)).save(log);
    }
}
