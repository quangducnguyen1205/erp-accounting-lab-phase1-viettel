package com.viettel.demo.storage;

import java.io.InputStream;

/*
 * Dữ liệu service trả cho controller khi download.
 * Controller sẽ dùng thông tin này để set Content-Type/filename và stream body.
 */
public record FileDownloadInfo(
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream content
) {
}
