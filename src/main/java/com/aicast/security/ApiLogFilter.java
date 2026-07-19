package com.aicast.security;

import com.aicast.domain.log.TbApiLog;
import com.aicast.service.log.ApiLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLogFilter extends OncePerRequestFilter {

    private final ApiLogService apiLogService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 파이프라인 변환 API 호출에 대해서만 관문 로그 수집
        return !path.startsWith("/api/process");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String corrId = MDC.get("correlationId");
        String apiKey = MDC.get("apiKey");
        
        String govName = (String) request.getAttribute("govName");
        if (govName == null) govName = "UNKNOWN";

        // 1. [선행 저장] 비즈니스 로직 진입 전에 부모 레코드를 동기 세이브하여 FK 충돌 원천 방지
        if (corrId != null && apiKey != null) {
            TbApiLog initialLog = TbApiLog.builder()
                    .corrId(corrId)
                    .apiKey(apiKey)
                    .govName(govName)
                    .endpoint(request.getRequestURI())
                    .clientIp(getClientIp(request))
                    .reqTime(LocalDateTime.now())
                    .isOk(false) // 기본은 false 상태로 둠
                    .build();
            apiLogService.saveLog(initialLog);
        }

        boolean isOk = true;
        String errMsg = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            isOk = false;
            errMsg = e.getMessage();
            throw e;
        } finally {
            long procMs = System.currentTimeMillis() - startTime;
            
            // HTTP Status 검사로 성공 여부 감지
            int status = response.getStatus();
            if (status >= 400) {
                isOk = false;
            }

            // 2. [사후 업데이트] 완료 후 소요 시간 및 최종 상태 업데이트
            if (corrId != null && apiKey != null) {
                apiLogService.updateLog(corrId, (int) procMs, isOk, errMsg);
                log.info("[API_LOG] Updated API Call Log - corrId: {}, apiKey: {}, endpoint: {}, procMs: {}, isOk: {}",
                        corrId, apiKey, request.getRequestURI(), procMs, isOk);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
