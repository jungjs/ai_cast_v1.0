package com.aicast.security;

import com.aicast.domain.gov.GovList;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private final ApiKeyService apiKeyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Actuator, Swagger, 정적 리소스, Ping API는 인증 제외
        return path.startsWith("/actuator") || 
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") ||
               path.equals("/api/ping");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        // API Key가 없는 경우
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API Key is missing for request: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "API Key is missing");
            return;
        }

        // 관리자용 통계/모니터링 API 호출 시 (F-13)
        if (request.getRequestURI().startsWith("/api/monitor")) {
            if (!apiKeyService.isAdmin(apiKey)) {
                log.warn("Forbidden access to admin API: {}", request.getRequestURI());
                response.sendError(HttpStatus.FORBIDDEN.value(), "Admin access required");
                return;
            }
        }

        // 일반 API 호출 시 gov_list 조회 및 인증 (F-06, F-07, F-08)
        Optional<GovList> govInfo = apiKeyService.validateAndGetGovInfo(apiKey);
        if (govInfo.isEmpty() && !apiKeyService.isAdmin(apiKey)) {
            log.warn("Invalid or inactive API Key for request: {}", request.getRequestURI());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API Key");
            return;
        }

        // 요청 속성에 지자체 정보 추가 (Controller에서 사용)
        govInfo.ifPresent(gov -> {
            request.setAttribute("govId", gov.getGovId());
            request.setAttribute("govName", gov.getGovName());
            org.slf4j.MDC.put("apiKey", apiKey);
            org.slf4j.MDC.put("govId", gov.getGovId());
        });

        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove("apiKey");
            org.slf4j.MDC.remove("govId");
        }
    }
}
