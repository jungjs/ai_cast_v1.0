package com.aicast.controller;

import com.aicast.dto.response.PipelineResponse;
import com.aicast.service.pipeline.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("AI Cast Pipeline is healthy");
    }

    @PostMapping("/process_audio")
    public ResponseEntity<PipelineResponse> processAudio(
            @RequestParam("file") MultipartFile audioFile,
            @RequestParam(value = "langs", defaultValue = "en,ja,zh-Hans") List<String> targetLangs) {
        
        log.info("Received process_audio request. file={}, langs={}", audioFile.getOriginalFilename(), targetLangs);
        
        // 1. WAV 파일 유효성 검증
        String filename = audioFile.getOriginalFilename();
        String contentType = audioFile.getContentType();
        boolean isValidWav = false;
        
        if (contentType != null && (contentType.equals("audio/wav") || contentType.equals("audio/x-wav") || contentType.equals("audio/wave"))) {
            isValidWav = true;
        } else if (filename != null && filename.toLowerCase().endsWith(".wav")) {
            isValidWav = true;
        }
        
        if (audioFile.isEmpty() || !isValidWav) {
            PipelineResponse errorResponse = PipelineResponse.builder()
                    .correlationId(java.util.UUID.randomUUID().toString())
                    .status("FAILED")
                    .errorMessage("[검증 오류] WAV 포맷 오디오 파일만 업로드할 수 있습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        PipelineResponse response = pipelineService.executeAudio(audioFile, targetLangs);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process_text")
    public ResponseEntity<PipelineResponse> processText(
            @RequestParam("text") String text,
            @RequestParam(value = "langs", defaultValue = "en,ja,zh-Hans") List<String> targetLangs) {
        
        log.info("Received process_text request. text={}, langs={}", text, targetLangs);
        PipelineResponse response = pipelineService.executeText(text, targetLangs);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process_img")
    public ResponseEntity<PipelineResponse> processImage(
            @RequestParam("file") MultipartFile imageFile,
            @RequestParam(value = "langs", defaultValue = "en,ja,zh-Hans") List<String> targetLangs) {
        
        log.info("Received process_img request. file={}, langs={}", imageFile.getOriginalFilename(), targetLangs);
        
        // 1. 이미지 용량 및 포맷 검증
        if (imageFile.isEmpty()) {
            PipelineResponse errorResponse = PipelineResponse.builder()
                    .correlationId(java.util.UUID.randomUUID().toString())
                    .status("FAILED")
                    .errorMessage("[검증 오류] 파일이 비어있습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        if (imageFile.getSize() > 20 * 1024 * 1024) {
            PipelineResponse errorResponse = PipelineResponse.builder()
                    .correlationId(java.util.UUID.randomUUID().toString())
                    .status("FAILED")
                    .errorMessage("[검증 오류] 이미지 파일 용량은 최대 20MB를 초과할 수 없습니다.")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        String filename = imageFile.getOriginalFilename();
        String ext = "";
        if (filename != null && filename.contains(".")) {
            ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        
        List<String> allowedExtensions = List.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".pdf", ".tiff");
        if (!allowedExtensions.contains(ext)) {
            PipelineResponse errorResponse = PipelineResponse.builder()
                    .correlationId(java.util.UUID.randomUUID().toString())
                    .status("FAILED")
                    .errorMessage("[검증 오류] 지원되지 않는 이미지 파일 형식입니다. (JPEG, PNG, GIF, BMP, WEBP, PDF, TIFF만 허용)")
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        PipelineResponse response = pipelineService.executeImage(imageFile, targetLangs);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
