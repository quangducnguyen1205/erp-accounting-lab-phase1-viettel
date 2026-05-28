package com.viettel.demo.storage;

import java.io.InputStream;
import java.util.Map;

/*
 * ==============================================================
 * FileStorageGateway — Gateway/Adapter cho object storage
 * ==============================================================
 *
 * [Vai trò]
 * Interface này mô tả các operation object storage mà service cần.
 * Implementation thật sau này sẽ gọi MinIO Java SDK.
 *
 * [Không làm ở gateway]
 * - Không đọc TenantContext.
 * - Không quyết định user có được download không.
 * - Không nhận request body trực tiếp.
 * - Không trả raw MinIO response ra controller.
 *
 * [Khi tự implement]
 * Tạo class kiểu MinioFileStorageGateway implements FileStorageGateway.
 *
 * ==============================================================
 */
public interface FileStorageGateway {

    StoredObjectInfo putObject(
            String objectKey,
            InputStream inputStream,
            long sizeBytes,
            String contentType,
            Map<String, String> metadata
    );

    InputStream getObject(String objectKey);

    StoredObjectInfo statObject(String objectKey);

    void removeObject(String objectKey);
}
