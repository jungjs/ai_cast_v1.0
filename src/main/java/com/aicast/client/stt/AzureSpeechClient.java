package com.aicast.client.stt;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class AzureSpeechClient implements SttClient {

    private final String subscriptionKey;
    private final String region;
    private final String language;

    public AzureSpeechClient(
            @Value("${aicast.azure.speech.key}") String subscriptionKey,
            @Value("${aicast.azure.speech.region}") String region) {
        this.subscriptionKey = subscriptionKey;
        this.region = region;
        this.language = "ko-KR"; // 기본 한국어
    }

    @Override
    public String speechToText(MultipartFile audioFile) throws Exception {
        // SDK는 파일 경로를 요구하므로 임시 파일로 저장
        File tempFile = File.createTempFile("audio-", ".wav");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(audioFile.getBytes());
        }

        try {
            return performContinuousRecognition(tempFile.getAbsolutePath());
        } finally {
            if (tempFile.exists() && !tempFile.delete()) {
                log.warn("임시 오디오 파일 삭제 실패: {}", tempFile.getAbsolutePath());
            }
        }
    }

    private String performContinuousRecognition(String filePath) throws Exception {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
        speechConfig.setSpeechRecognitionLanguage(language);

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(filePath);
        Semaphore stopSemaphore = new Semaphore(0);
        List<String> recognizedTexts = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    recognizedTexts.add(e.getResult().getText());
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                if (e.getReason() == CancellationReason.Error) {
                    errors.add("STT Error: " + e.getErrorDetails());
                }
                stopSemaphore.release();
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                stopSemaphore.release();
            });

            recognizer.startContinuousRecognitionAsync().get();
            stopSemaphore.acquire();
            recognizer.stopContinuousRecognitionAsync().get();
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(String.join("; ", errors));
        }

        return String.join(" ", recognizedTexts);
    }
}
