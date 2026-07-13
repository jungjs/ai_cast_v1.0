package com.aicast.security;

import com.aicast.domain.gov.GovList;
import com.aicast.domain.gov.GovListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final GovListRepository govListRepository;

    // 관리자용 공통 API Key 접두사 또는 고정값 (임시 설계)
    private static final String ADMIN_KEY_PREFIX = "ADMIN-";

    /**
     * API Key 유효성 검사 및 GovList 반환 (F-08)
     */
    @Transactional(readOnly = true)
    public Optional<GovList> validateAndGetGovInfo(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        
        // gov_list 테이블에서 조회 (isActive = true 조건 포함)
        return govListRepository.findByApiKeyAndIsActiveTrue(apiKey);
    }

    /**
     * 관리자 여부 확인 (F-13)
     */
    public boolean isAdmin(String apiKey) {
        return apiKey != null && apiKey.startsWith(ADMIN_KEY_PREFIX);
    }
}
