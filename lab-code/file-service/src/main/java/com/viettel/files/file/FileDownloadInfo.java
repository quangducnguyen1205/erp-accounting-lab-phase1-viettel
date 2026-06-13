package com.viettel.files.file;

import java.io.InputStream;

public record FileDownloadInfo(
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream content
) {
}
