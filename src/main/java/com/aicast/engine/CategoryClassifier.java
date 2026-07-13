package com.aicast.engine;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CategoryClassifier {
    
    private static final List<String> DISASTER_KEYWORDS = List.of("폭우", "지진", "태풍", "화재", "대피", "경보", "주의보");
    private static final List<String> NOTICE_KEYWORDS = List.of("공지", "안내", "변경", "통제");

    public String classify(String text) {
        if (text == null) return "INFO";
        
        for (String keyword : DISASTER_KEYWORDS) {
            if (text.contains(keyword)) return "DISASTER";
        }
        for (String keyword : NOTICE_KEYWORDS) {
            if (text.contains(keyword)) return "NOTICE";
        }
        
        return "INFO"; // 기본값 (알림)
    }
}
