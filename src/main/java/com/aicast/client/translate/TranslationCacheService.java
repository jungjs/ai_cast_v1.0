package com.aicast.client.translate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.concurrent.TimeUnit;

@Service
public class TranslationCacheService {
    
    private final Cache<String, String> translationCache;
    
    public TranslationCacheService(
            @Value("${aicast.azure.translator.cache.max-size:10000}") int maxSize,
            @Value("${aicast.azure.translator.cache.ttl-hours:24}") int ttlHours) {
        this.translationCache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlHours, TimeUnit.HOURS)
            .build();
    }
    
    public String get(String sourceText, String targetLang) {
        String key = generateKey(sourceText, targetLang);
        return translationCache.getIfPresent(key);
    }
    
    public void put(String sourceText, String targetLang, String translatedText) {
        String key = generateKey(sourceText, targetLang);
        translationCache.put(key, translatedText);
    }
    
    private String generateKey(String text, String lang) {
        return DigestUtils.md5DigestAsHex((text + ":" + lang).getBytes());
    }
}
