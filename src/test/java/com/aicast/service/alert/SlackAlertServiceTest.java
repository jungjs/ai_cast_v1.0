package com.aicast.service.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackAlertServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private SlackAlertService slackAlertService;
    private final String webhookUrl = "http://mock-slack-webhook.com";

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        slackAlertService = new SlackAlertService(webClientBuilder, webhookUrl);
    }

    @Test
    @DisplayName("Webhook URL이 비어 있을 때 전송 생략 검증")
    void sendAlert_EmptyWebhookUrl() {
        // Given
        SlackAlertService serviceWithNoUrl = new SlackAlertService(webClientBuilder, "");

        // When
        serviceWithNoUrl.sendAlert(SlackAlertService.AlertLevel.WARNING, "테스트 경고", "corr-123");

        // Then
        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("정상 상황에서 WebClient를 통한 슬랙 알림 발송 검증")
    void sendAlert_Success() {
        // Given
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(webhookUrl)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("ok"));

        // When
        slackAlertService.sendAlert(SlackAlertService.AlertLevel.ERROR, "서버 에러 발생", "corr-error");

        // Then
        verify(webClient, times(1)).post();
    }

    @Test
    @DisplayName("60초 이내 동일 레벨/메시지 발송 시 디바운스 필터링(억제) 검증")
    void sendAlert_DebounceSuppressed() {
        // Given
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(webhookUrl)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("ok"));

        // When
        // 1차 발송
        slackAlertService.sendAlert(SlackAlertService.AlertLevel.CRITICAL, "임계치 초과", "corr-critical");
        // 2차 발송 (동일 레벨, 동일 메시지)
        slackAlertService.sendAlert(SlackAlertService.AlertLevel.CRITICAL, "임계치 초과", "corr-critical");

        // Then
        verify(webClient, times(1)).post();
    }
}
