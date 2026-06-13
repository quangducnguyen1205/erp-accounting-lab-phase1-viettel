package com.viettel.files.file;

public record FileUploadResponse(
        String fileId,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}
