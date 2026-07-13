package com.aicast.client.storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Slf4j
@Component
public class AzureBlobClient implements BlobClient {

    private final BlobContainerClient containerClient;

    public AzureBlobClient(
            @Value("${aicast.azure.storage.connection-string:}") String connectionString,
            @Value("${aicast.azure.storage.container-name:aicast-images}") String containerName) {
        
        if (connectionString != null && !connectionString.isEmpty()) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            this.containerClient = blobServiceClient.getBlobContainerClient(containerName);
            // 컨테이너가 없으면 생성
            if (!this.containerClient.exists()) {
                this.containerClient.create();
            }
        } else {
            this.containerClient = null;
        }
    }

    @Override
    public String upload(byte[] pngBytes, String fileName) throws Exception {
        if (containerClient == null) {
            log.warn("Azure Blob Storage is not configured. Returning mock URL.");
            return "https://mock-storage.blob.core.windows.net/aicast-images/" + fileName;
        }

        BlockBlobClient blobClient = containerClient.getBlobClient(fileName).getBlockBlobClient();
        
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(pngBytes)) {
            blobClient.upload(dataStream, pngBytes.length, true);
        }

        // Public Access가 허용된 컨테이너라고 가정하고 기본 URL 반환
        return blobClient.getBlobUrl();
    }
}
