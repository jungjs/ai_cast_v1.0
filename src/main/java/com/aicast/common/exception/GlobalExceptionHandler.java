package com.aicast.common.exception;

import com.aicast.service.alert.SlackAlertService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SlackAlertService slackAlertService;

    @ExceptionHandler(AzureServiceException.class)
    public ResponseEntity<ErrorResponse> handleAzureError(AzureServiceException e) {
        String correlationId = MDC.get("correlationId");
        
        log.error("Azure Service Failed [{}] - correlationId: {}", e.getServiceType(), correlationId, e);
        
        slackAlertService.sendAlert(
            SlackAlertService.AlertLevel.ERROR,
            e.getMessage(),
            correlationId
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(correlationId, e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException e) {
        String correlationId = MDC.get("correlationId");
        log.warn("Resource Not Found - path: {}, correlationId: {}", e.getResourcePath(), correlationId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(correlationId, "요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        String correlationId = MDC.get("correlationId");
        
        log.error("Unexpected Error - correlationId: {}", correlationId, e);
        
        slackAlertService.sendAlert(
            SlackAlertService.AlertLevel.CRITICAL,
            "시스템 내부 오류: " + e.getMessage(),
            correlationId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(correlationId, "내부 서버 오류가 발생했습니다."));
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String correlationId;
        private String errorMessage;
    }
}
