package com.aicast.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryClassifierTest {

    private final CategoryClassifier categoryClassifier = new CategoryClassifier();

    @Test
    @DisplayName("재난 관련 키워드 포함 시 DISASTER 분류 검증")
    void classify_Disaster() {
        // Given & When & Then
        assertEquals("DISASTER", categoryClassifier.classify("오늘 밤 폭우가 내릴 예정입니다."));
        assertEquals("DISASTER", categoryClassifier.classify("지진 대피 요령을 숙지하세요."));
        assertEquals("DISASTER", categoryClassifier.classify("태풍 경보가 발령되었습니다."));
    }

    @Test
    @DisplayName("공지/안내 관련 키워드 포함 시 NOTICE 분류 검증")
    void classify_Notice() {
        // Given & When & Then
        assertEquals("NOTICE", categoryClassifier.classify("주민 센터 정기 휴무 안내입니다."));
        assertEquals("NOTICE", categoryClassifier.classify("버스 노선 변경 공지"));
    }

    @Test
    @DisplayName("일반 텍스트 또는 Null 입력 시 INFO 분류 검증")
    void classify_Info() {
        // Given & When & Then
        assertEquals("INFO", categoryClassifier.classify("오늘 날씨는 아주 맑고 화창합니다."));
        assertEquals("INFO", categoryClassifier.classify(null));
        assertEquals("INFO", categoryClassifier.classify(""));
    }
}
