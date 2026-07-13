package com.aicast.common.util;

public class ApiKeyMasker {

    private ApiKeyMasker() {
        // Utility class
    }

    /**
     * API Key를 로깅용으로 마스킹합니다. (NF-04)
     * 예: "1234567890abcdef" -> "1234***cdef"
     */
    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}
