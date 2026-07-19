package com.aicast.service.log;

import com.aicast.domain.log.TbApiLog;
import com.aicast.domain.log.TbApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogService {

    private final TbApiLogRepository apiLogRepository;

    public void saveLog(TbApiLog tbApiLog) {
        try {
            apiLogRepository.save(tbApiLog);
        } catch (Exception e) {
            log.error("Failed to save API log", e);
        }
    }

    @Transactional
    public void updateLog(String corrId, int procMs, boolean isOk, String errMsg) {
        try {
            apiLogRepository.findById(corrId).ifPresent(log -> {
                log.setProcMs(procMs);
                log.setIsOk(isOk);
                log.setResTime(LocalDateTime.now());
                if (errMsg != null) {
                    log.setErrMsg(errMsg);
                }
                apiLogRepository.save(log);
            });
        } catch (Exception e) {
            log.error("Failed to update API log - corrId: {}", corrId, e);
        }
    }
}
