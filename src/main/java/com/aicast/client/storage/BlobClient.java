package com.aicast.client.storage;

public interface BlobClient {
    /**
     * PNG 바이트 배열을 Blob Storage에 업로드하고 접근 가능한 URL을 반환합니다.
     * @param pngBytes 생성된 이미지 바이트
     * @param fileName 업로드할 파일명 (확장자 포함)
     * @return 생성된 이미지의 URL (SAS Token 포함 또는 Public URL)
     */
    String upload(byte[] pngBytes, String fileName) throws Exception;
}
