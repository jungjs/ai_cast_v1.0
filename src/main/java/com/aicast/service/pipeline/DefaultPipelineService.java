package com.aicast.service.pipeline;

import com.aicast.client.nlp.NlpClient;
import com.aicast.client.nlp.NlpResult;
import com.aicast.client.ocr.OcrClient;
import com.aicast.client.ocr.OcrResult;
import com.aicast.client.storage.BlobClient;
import com.aicast.client.stt.SttClient;
import com.aicast.client.translate.TranslateClient;
import com.aicast.client.translate.TranslationResult;
import com.aicast.dto.response.PipelineResponse;
import com.aicast.engine.ImageRenderingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultPipelineService implements PipelineService {

    private final SttClient sttClient;
    private final NlpClient nlpClient;
    private final TranslateClient translateClient;
    private final OcrClient ocrClient;
    private final BlobClient blobClient;
    private final ImageRenderingEngine imageEngine;

    @Override
    public PipelineResponse executeAudio(MultipartFile audioFile, List<String> targetLangs) {
        PipelineResponse.PipelineResponseBuilder builder = initResponse();
        Map<String, Long> times = new HashMap<>();

        try {
            // 1. STT
            long start = System.currentTimeMillis();
            String sttText = sttClient.speechToText(audioFile);
            times.put("STT", System.currentTimeMillis() - start);
            builder.originalText(sttText);

            // 2. NLP (정제 + 요약)
            NlpResult nlpResult = processNlp(sttText, times);
            builder.refinedText(nlpResult.getRefinedText()).summary(nlpResult.getSummary());

            // 3. 번역
            TranslationResult transResult = processTranslation(nlpResult.getSummary(), targetLangs, times);
            builder.translations(transResult.getTranslations());

            // 4. 이미지 렌더링 및 5. 업로드
            String imageUrl = renderAndUpload(transResult.getTranslations(), nlpResult.getRefinedText(), times);
            builder.imageUrl(imageUrl);

            builder.status("SUCCESS");
        } catch (Exception e) {
            log.error("Audio Pipeline Error", e);
            builder.status("FAILED").errorMessage(e.getMessage());
        }

        builder.processingTimesMs(times);
        return builder.build();
    }

    @Override
    public PipelineResponse executeText(String text, List<String> targetLangs) {
        PipelineResponse.PipelineResponseBuilder builder = initResponse();
        Map<String, Long> times = new HashMap<>();
        builder.originalText(text);

        try {
            NlpResult nlpResult = processNlp(text, times);
            builder.refinedText(nlpResult.getRefinedText()).summary(nlpResult.getSummary());

            TranslationResult transResult = processTranslation(nlpResult.getSummary(), targetLangs, times);
            builder.translations(transResult.getTranslations());

            String imageUrl = renderAndUpload(transResult.getTranslations(), nlpResult.getRefinedText(), times);
            builder.imageUrl(imageUrl);

            builder.status("SUCCESS");
        } catch (Exception e) {
            log.error("Text Pipeline Error", e);
            builder.status("FAILED").errorMessage(e.getMessage());
        }

        builder.processingTimesMs(times);
        return builder.build();
    }

    @Override
    public PipelineResponse executeImage(MultipartFile imageFile, List<String> targetLangs) {
        PipelineResponse.PipelineResponseBuilder builder = initResponse();
        Map<String, Long> times = new HashMap<>();

        try {
            // 1. OCR
            long start = System.currentTimeMillis();
            OcrResult ocrResult = ocrClient.extractText(imageFile);
            times.put("OCR", System.currentTimeMillis() - start);
            
            if (!"SUCCESS".equals(ocrResult.getStatus())) {
                throw new RuntimeException("OCR Failed: " + ocrResult.getStatus());
            }
            builder.originalText(ocrResult.getExtractedText());

            // 2. NLP
            NlpResult nlpResult = processNlp(ocrResult.getExtractedText(), times);
            builder.refinedText(nlpResult.getRefinedText()).summary(nlpResult.getSummary());

            // 3. 번역
            TranslationResult transResult = processTranslation(nlpResult.getSummary(), targetLangs, times);
            builder.translations(transResult.getTranslations());

            // 4. 이미지 렌더링 및 업로드
            String imageUrl = renderAndUpload(transResult.getTranslations(), nlpResult.getRefinedText(), times);
            builder.imageUrl(imageUrl);

            builder.status("SUCCESS");
        } catch (Exception e) {
            log.error("Image Pipeline Error", e);
            builder.status("FAILED").errorMessage(e.getMessage());
        }

        builder.processingTimesMs(times);
        return builder.build();
    }

    private PipelineResponse.PipelineResponseBuilder initResponse() {
        return PipelineResponse.builder()
                .correlationId(MDC.get("correlationId"));
    }

    private NlpResult processNlp(String text, Map<String, Long> times) {
        NlpResult result = nlpClient.processText(text);
        times.put("NLP", result.getProcessingTimeMs());
        if (!"SUCCESS".equals(result.getStatus())) {
            throw new RuntimeException("NLP Failed: " + result.getStatus());
        }
        return result;
    }

    private TranslationResult processTranslation(String summary, List<String> targetLangs, Map<String, Long> times) {
        long start = System.currentTimeMillis();
        TranslationResult result = translateClient.translate(summary, "ko", targetLangs);
        times.put("TRANSLATE", System.currentTimeMillis() - start);
        if (!"SUCCESS".equals(result.getStatus())) {
             throw new RuntimeException("Translate Failed: " + result.getStatus());
        }
        return result;
    }

    private String renderAndUpload(Map<String, String> translations, String originalText, Map<String, Long> times) throws Exception {
        long start = System.currentTimeMillis();
        byte[] pngBytes = imageEngine.render(translations, originalText);
        times.put("ENGINE", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        String fileName = UUID.randomUUID().toString() + ".png";
        String url = blobClient.upload(pngBytes, fileName);
        times.put("STORAGE", System.currentTimeMillis() - start);
        return url;
    }
}
