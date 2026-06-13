package com.viettel.files.file;

import java.io.InputStream;
import java.util.Map;

public interface FileStorageGateway {

    StoredObjectInfo putObject(
            String objectKey,
            InputStream inputStream,
            long sizeBytes,
            String contentType,
            Map<String, String> metadata
    );

    InputStream getObject(String objectKey);

    void removeObject(String objectKey);
}
