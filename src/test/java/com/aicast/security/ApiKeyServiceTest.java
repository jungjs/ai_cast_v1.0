package com.aicast.security;

import com.aicast.domain.gov.GovList;
import com.aicast.domain.gov.GovListRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private GovListRepository govListRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    @DisplayName("유효한 API Key로 지자체 정보 조회 성공 검증")
    void validateAndGetGovInfo_Success() {
        // Given
        String apiKey = "valid-key-12345";
        GovList mockGov = mock(GovList.class);
        when(mockGov.getGovId()).thenReturn("gov-01");
        when(mockGov.getGovName()).thenReturn("테스트지자체");

        when(govListRepository.findByApiKeyAndIsActiveTrue(apiKey)).thenReturn(Optional.of(mockGov));

        // When
        Optional<GovList> result = apiKeyService.validateAndGetGovInfo(apiKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals("gov-01", result.get().getGovId());
        assertEquals("테스트지자체", result.get().getGovName());
        verify(govListRepository, times(1)).findByApiKeyAndIsActiveTrue(apiKey);
    }

    @Test
    @DisplayName("유효하지 않거나 비활성화된 API Key로 지자체 정보 조회 실패 검증")
    void validateAndGetGovInfo_InvalidOrInactive() {
        // Given
        String apiKey = "invalid-key-999";
        when(govListRepository.findByApiKeyAndIsActiveTrue(apiKey)).thenReturn(Optional.empty());

        // When
        Optional<GovList> result = apiKeyService.validateAndGetGovInfo(apiKey);

        // Then
        assertFalse(result.isPresent());
        verify(govListRepository, times(1)).findByApiKeyAndIsActiveTrue(apiKey);
    }

    @Test
    @DisplayName("API Key가 Null 또는 빈 값인 경우 조회 생략 검증")
    void validateAndGetGovInfo_NullOrEmpty() {
        // When & Then
        assertFalse(apiKeyService.validateAndGetGovInfo(null).isPresent());
        assertFalse(apiKeyService.validateAndGetGovInfo("").isPresent());
        assertFalse(apiKeyService.validateAndGetGovInfo("   ").isPresent());

        verifyNoInteractions(govListRepository);
    }

    @Test
    @DisplayName("관리자 키(ADMIN- 접두사) 여부 검증")
    void isAdmin_Verification() {
        // When & Then
        assertTrue(apiKeyService.isAdmin("ADMIN-key-123"));
        assertTrue(apiKeyService.isAdmin("ADMIN-"));
        assertFalse(apiKeyService.isAdmin("USER-key-123"));
        assertFalse(apiKeyService.isAdmin(""));
        assertFalse(apiKeyService.isAdmin(null));
    }
}
