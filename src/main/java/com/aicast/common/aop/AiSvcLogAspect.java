package com.aicast.common.aop;

import com.aicast.domain.log.TbAiSvcLog;
import com.aicast.service.log.AiSvcLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AiSvcLogAspect {

    private final AiSvcLogService aiSvcLogService;

    @Around("execution(* com.aicast.client..*.*(..))")
    public Object logAiServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String svcType = deriveSvcType(className);
        
        String corrId = MDC.get("correlationId");
        String apiKey = MDC.get("apiKey");
        
        if (corrId == null) corrId = "UNKNOWN";
        if (apiKey == null) apiKey = "UNKNOWN";

        boolean isOk = true;
        String errMsg = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            isOk = false;
            errMsg = t.getMessage();
            throw t;
        } finally {
            long procMs = System.currentTimeMillis() - start;
            
            TbAiSvcLog aiSvcLog = TbAiSvcLog.builder()
                    .corrId(corrId)
                    .apiKey(apiKey)
                    .svcType(svcType)
                    .procMs((int) procMs)
                    .isOk(isOk)
                    .errMsg(errMsg)
                    .build();
                    
            aiSvcLogService.saveLog(aiSvcLog);
            
            log.info("[AI_SVC_LOG] {} - {}ms, isOk: {}", svcType, procMs, isOk);
        }
    }

    private String deriveSvcType(String className) {
        if (className.contains("Speech")) return "STT";
        if (className.contains("OpenAI")) return "NLP";
        if (className.contains("Translator")) return "TRANSLATE";
        if (className.contains("Vision")) return "OCR";
        if (className.contains("Blob")) return "STORAGE";
        return "UNKNOWN";
    }
}
