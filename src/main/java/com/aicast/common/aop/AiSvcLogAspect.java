package com.aicast.common.aop;

import com.aicast.client.nlp.NlpResult;
import com.aicast.client.translate.TranslationResult;
import com.aicast.domain.log.TbAiSvcLog;
import com.aicast.service.log.AiSvcLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AiSvcLogAspect {

    private final AiSvcLogService aiSvcLogService;

    @Around("execution(* com.aicast.client..*Client.*(..))")
    public Object logAiServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String svcType = deriveSvcType(joinPoint);
        String methodName = joinPoint.getSignature().getName();
        
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

            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;
            Integer reqSize = null;
            Integer resSize = null;

            if (result instanceof NlpResult) {
                NlpResult nlp = (NlpResult) result;
                promptTokens = nlp.getPromptTokens();
                completionTokens = nlp.getCompletionTokens();
                totalTokens = nlp.getTotalTokens();
            } else if (result instanceof TranslationResult) {
                TranslationResult tr = (TranslationResult) result;
                promptTokens = null;
                completionTokens = null;
                totalTokens = null;
                reqSize = tr.getPromptTokens(); // 번역 원문 글자 수
                resSize = tr.getCompletionTokens(); // 번역 완료 텍스트 글자 수
            }

            if (result != null && resSize == null) {
                resSize = result.toString().getBytes(StandardCharsets.UTF_8).length;
            }

            TbAiSvcLog aiSvcLog = TbAiSvcLog.builder()
                    .corrId(corrId)
                    .apiKey(apiKey)
                    .svcType(svcType)
                    .procMs((int) procMs)
                    .isOk(isOk)
                    .errMsg(errMsg)
                    .reqSize(reqSize)
                    .resSize(resSize)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .build();
                    
            aiSvcLogService.saveLog(aiSvcLog);
            
            log.info("[AI_SVC_LOG] {} - {}ms, isOk: {}, tokens: {}/{}/{}", 
                    svcType, procMs, isOk, promptTokens, completionTokens, totalTokens);
        }
    }

    private String deriveSvcType(ProceedingJoinPoint joinPoint) {
        // 1. 선언부 인터페이스 타입 기준 판별 (프록시의 영향을 전혀 받지 않음)
        Class<?> declaringType = joinPoint.getSignature().getDeclaringType();
        String typeName = declaringType.getSimpleName();

        if (typeName.contains("Speech") || typeName.contains("Stt")) return "STT";
        if (typeName.contains("OpenAI") || typeName.contains("Nlp")) return "NLP";
        if (typeName.contains("Translator") || typeName.contains("Translate")) return "TRANSLATE";
        if (typeName.contains("Vision") || typeName.contains("Ocr")) return "OCR";
        if (typeName.contains("Blob") || typeName.contains("Storage")) return "STORAGE";

        // 2. 만약 선언부 타입으로 판별이 어려울 경우 구체 클래스명 기준 2차 판별
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.contains("Speech") || className.contains("Stt")) return "STT";
        if (className.contains("OpenAI") || className.contains("Nlp")) return "NLP";
        if (className.contains("Translator") || className.contains("Translate")) return "TRANSLATE";
        if (className.contains("Vision") || className.contains("Ocr")) return "OCR";
        if (className.contains("Blob") || className.contains("Storage")) return "STORAGE";

        return "UNKNOWN";
    }
}
