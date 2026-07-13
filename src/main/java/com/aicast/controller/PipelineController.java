package com.aicast.controller;

import com.aicast.dto.response.PipelineResponse;
import com.aicast.service.pipeline.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        PipelineResponse response = pipelineService.executeAudio(audioFile, targetLangs);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process_text")
    public ResponseEntity<PipelineResponse> processText(
            @RequestParam("text") String text,
            @RequestParam(value = "langs", defaultValue = "en,ja,zh-Hans") List<String> targetLangs) {
        
        log.info("Received process_text request. text={}, langs={}", text, targetLangs);
        PipelineResponse response = pipelineService.executeText(text, targetLangs);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process_img")
    public ResponseEntity<PipelineResponse> processImage(
            @RequestParam("file") MultipartFile imageFile,
            @RequestParam(value = "langs", defaultValue = "en,ja,zh-Hans") List<String> targetLangs) {
        
        log.info("Received process_img request. file={}, langs={}", imageFile.getOriginalFilename(), targetLangs);
        PipelineResponse response = pipelineService.executeImage(imageFile, targetLangs);
        return ResponseEntity.ok(response);
    }
}
