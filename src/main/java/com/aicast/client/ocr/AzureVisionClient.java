package com.aicast.client.ocr;

import com.azure.ai.vision.imageanalysis.ImageAnalysisClient;
import com.azure.ai.vision.imageanalysis.ImageAnalysisClientBuilder;
import com.azure.ai.vision.imageanalysis.models.ImageAnalysisResult;
import com.azure.ai.vision.imageanalysis.models.VisualFeatures;
import com.azure.core.credential.KeyCredential;
import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Slf4j
@Component
public class AzureVisionClient implements OcrClient {

    private final ImageAnalysisClient client;
    private final double minConfidence;

    public AzureVisionClient(
            @Value("${aicast.azure.vision.endpoint:}") String endpoint,
            @Value("${aicast.azure.vision.key:}") String key,
            @Value("${aicast.azure.vision.ocr.min-confidence:0.7}") double minConfidence) {
        
        ImageAnalysisClientBuilder builder = new ImageAnalysisClientBuilder();
        if (endpoint != null && !endpoint.isEmpty() && key != null && !key.isEmpty()) {
             builder.endpoint(endpoint).credential(new KeyCredential(key));
        }
        this.client = builder.buildClient();
        this.minConfidence = minConfidence;
    }

    @Override
    public OcrResult extractText(MultipartFile imageFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            byte[] imageBytes = imageFile.getBytes();
            
            // 이미지 분석 수행 (READ 기능만 사용)
            ImageAnalysisResult result = client.analyze(
                BinaryData.fromBytes(imageBytes),
                Arrays.asList(VisualFeatures.READ),
                null  // ImageAnalysisOptions
            );
            
            // 텍스트 추출 및 신뢰도 필터링
            StringBuilder extractedText = new StringBuilder();
            
            if (result.getRead() != null) {
                result.getRead().getBlocks().forEach(block -> {
                    block.getLines().forEach(line -> {
                        // 라인별 평균 신뢰도 계산
                        double avgConfidence = line.getWords().stream()
                            .mapToDouble(w -> w.getConfidence())
                            .average().orElse(0.0);
                        
                        if (avgConfidence >= minConfidence) {
                            extractedText.append(line.getText()).append("\n");
                        }
                    });
                });
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("OCR 처리 완료 - 소요시간: {}ms", elapsed);
            
            return new OcrResult(extractedText.toString().trim(), elapsed, "SUCCESS");
            
        } catch (Exception e) {
            log.error("OCR 처리 실패: {}", e.getMessage(), e);
            long elapsed = System.currentTimeMillis() - startTime;
            return new OcrResult(null, elapsed, "FAILED: " + e.getMessage());
        }
    }
}
