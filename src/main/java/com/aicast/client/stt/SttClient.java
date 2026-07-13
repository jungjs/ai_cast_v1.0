package com.aicast.client.stt;

import org.springframework.web.multipart.MultipartFile;

public interface SttClient {
    /**
     * 오디오 파일에서 텍스트를 추출합니다.
     * @param audioFile 오디오 파일 (WAV 등)
     * @return 추출된 텍스트
     */
    String speechToText(MultipartFile audioFile) throws Exception;
}
