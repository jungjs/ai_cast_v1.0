package com.aicast.service.log;

import com.aicast.domain.log.TbAiSvcLog;
import com.aicast.domain.log.TbAiSvcLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSvcLogService {

    private final TbAiSvcLogRepository aiSvcLogRepository;

    @Async
    public void saveLog(TbAiSvcLog tbAiSvcLog) {
        try {
            aiSvcLogRepository.save(tbAiSvcLog);
        } catch (Exception e) {
            log.error("Failed to save AI Service log asynchronously", e);
        }
    }
}
