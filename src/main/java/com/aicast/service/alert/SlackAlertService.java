package com.aicast.service.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SlackAlertService {

    private final String webhookUrl;
    private final WebClient webClient;
    private final Map<String, Long> lastSentTimes = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 60000; // 60초 동일 메시지 발송 억제

    public SlackAlertService(
            WebClient.Builder builder,
            @Value("${slack.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.webClient = builder.build();
    }

    public enum AlertLevel {
        WARNING, ERROR, CRITICAL
    }

    public void sendAlert(AlertLevel level, String message, String correlationId) {
        // 디바운싱: 동일한 메시지는 60초 내 재발송 안함
        long now = System.currentTimeMillis();
        String cacheKey = level.name() + ":" + message;
        Long lastSent = lastSentTimes.get(cacheKey);
        
        if (lastSent != null && (now - lastSent) < DEBOUNCE_MS) {
            log.debug("Slack Alert suppressed due to debouncing: {}", message);
            return;
        }

        lastSentTimes.put(cacheKey, now);

        String emoji = level == AlertLevel.CRITICAL ? "🔴" : (level == AlertLevel.ERROR ? "🟠" : "🟡");
        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String payload = String.format("""
            {
              "blocks": [{
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "%s *[AI Cast %s]*\\n• *메시지:* %s\\n• *CorrelationId:* `%s`\\n• *시각:* %s"
                }
              }]
            }""",
            emoji, level, message, correlationId, formattedTime
        );

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[MOCK SLACK] Webhook URL is empty. Would have sent: {}", payload);
            return;
        }

        try {
            webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("Slack alert failed", e))
                .subscribe();
        } catch (Exception e) {
            log.error("Failed to send Slack alert", e);
        }
    }
}
