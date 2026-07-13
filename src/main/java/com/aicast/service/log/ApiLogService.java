package com.aicast.service.log;

import com.aicast.domain.log.TbApiLog;
import com.aicast.domain.log.TbApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogService {

    private final TbApiLogRepository apiLogRepository;

    @Async
    public void saveLog(TbApiLog tbApiLog) {
        try {
            apiLogRepository.save(tbApiLog);
        } catch (Exception e) {
            log.error("Failed to save API log asynchronously", e);
        }
    }
}
