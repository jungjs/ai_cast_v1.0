package com.aicast.client.ocr;

import org.springframework.web.multipart.MultipartFile;

public interface OcrClient {
    /**
     * 이미지 파일에서 텍스트를 추출합니다.
     * @param imageFile 이미지 파일 (JPG 등)
     * @return 추출된 텍스트 결과
     */
    OcrResult extractText(MultipartFile imageFile);
}
