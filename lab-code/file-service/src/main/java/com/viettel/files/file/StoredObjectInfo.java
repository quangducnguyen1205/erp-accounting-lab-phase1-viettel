package com.viettel.files.file;

public record StoredObjectInfo(
        String bucket,
        String objectKey,
        String etag,
        String contentType,
        long sizeBytes
) {
}
