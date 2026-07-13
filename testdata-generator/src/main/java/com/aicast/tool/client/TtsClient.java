package com.aicast.tool.client;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class TtsClient implements AutoCloseable {

    private final SpeechConfig speechConfig;
    private final SpeechSynthesizer synthesizer;

    public TtsClient(String subscriptionKey, String region) {
        this.speechConfig = SpeechConfig.fromSubscription(subscriptionKey, region);
        this.speechConfig.setSpeechSynthesisVoiceName("ko-KR-SunHiNeural");
        this.synthesizer = new SpeechSynthesizer(speechConfig, null);
    }

    public void synthesizeToWav(String text, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            AudioConfig audioConfig = AudioConfig.fromWavFileOutput(outputPath.toString());
            SpeechSynthesizer fileSynthesizer = new SpeechSynthesizer(speechConfig, audioConfig);

            try {
                SpeechSynthesisResult result = fileSynthesizer.SpeakText(text);
                if (result.getReason() == com.microsoft.cognitiveservices.speech.ResultReason.SynthesizingAudioCompleted) {
                    log.info("TTS 성공: {} ({} bytes)", outputPath.getFileName(), result.getAudioLength());
                    return;
                } else {
                    log.warn("TTS 실패 (시도 {}/{}): {} - {}", attempt, maxRetries, outputPath.getFileName(), result.getReason());
                    if (attempt == maxRetries) {
                        throw new RuntimeException("TTS synthesis failed after " + maxRetries + " attempts: " + result.getReason());
                    }
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                audioConfig.close();
                fileSynthesizer.close();
            }
        }
    }

    @Override
    public void close() {
        synthesizer.close();
        speechConfig.close();
    }
}
