package com.aicast.client.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobClientTest {

    @Test
    @DisplayName("커넥션 정보 미설정 시 가짜 저장소 URL 반환 검증")
    void upload_NotConfigured() throws Exception {
        // Given
        AzureBlobClient client = new AzureBlobClient("", "aicast-images");
        byte[] bytes = {1, 2, 3};
        String fileName = "test.png";

        // When
        String url = client.upload(bytes, fileName);

        // Then
        assertNotNull(url);
        assertEquals("https://mock-storage.blob.core.windows.net/aicast-images/test.png", url);
    }

    @Test
    @DisplayName("컨테이너 클라이언트가 주입되었을 때 정상 업로드 및 URL 반환 검증")
    void upload_Success() throws Exception {
        // Given
        AzureBlobClient client = new AzureBlobClient("", "aicast-images");
        
        BlobContainerClient mockContainerClient = mock(BlobContainerClient.class);
        com.azure.storage.blob.BlobClient mockBlobClient = mock(com.azure.storage.blob.BlobClient.class);
        BlockBlobClient mockBlockBlobClient = mock(BlockBlobClient.class);
        
        ReflectionTestUtils.setField(client, "containerClient", mockContainerClient);
        
        when(mockContainerClient.getBlobClient(anyString())).thenReturn(mockBlobClient);
        when(mockBlobClient.getBlockBlobClient()).thenReturn(mockBlockBlobClient);
        
        when(mockBlockBlobClient.getBlobUrl()).thenReturn("https://real-azure.blob.core.windows.net/aicast-images/test.png");

        byte[] bytes = {1, 2, 3};
        String fileName = "test.png";

        // When
        String url = client.upload(bytes, fileName);

        // Then
        assertNotNull(url);
        assertEquals("https://real-azure.blob.core.windows.net/aicast-images/test.png", url);
        verify(mockBlockBlobClient, times(1)).upload(any(InputStream.class), eq(3L), eq(true));
        verify(mockBlockBlobClient, times(1)).getBlobUrl();
    }
}
