package com.viettel.demo.storage;

/*
 * Thông tin an toàn trả về từ object storage sau put/stat.
 * Không chứa secret, presigned URL dài hoặc raw SDK response.
 */
public record StoredObjectInfo(
        String bucket,
        String objectKey,
        String etag,
        String contentType,
        long sizeBytes
) {
}
