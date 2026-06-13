package com.viettel.files.file;

import java.time.LocalDateTime;

public record FileMetadataResponse(
        String fileId,
        Long tenantId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        LocalDateTime createdAt
) {
    static FileMetadataResponse from(FileMetadata metadata) {
        return new FileMetadataResponse(
                metadata.getFileId(),
                metadata.getTenantId(),
                metadata.getOriginalFilename(),
                metadata.getContentType(),
                metadata.getSizeBytes(),
                metadata.getCreatedAt()
        );
    }
}
