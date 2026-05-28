package com.viettel.demo.storage;

/*
 * Response an toàn sau upload.
 * Phase sau có thể đổi fileId từ String sang UUID/Long tùy metadata table.
 */
public record FileUploadResponse(
        String fileId,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}
