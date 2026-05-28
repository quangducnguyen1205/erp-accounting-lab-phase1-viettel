package com.viettel.demo.storage;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * ==============================================================
 * MinioClientConfig — cấu hình MinIO Java SDK
 * ==============================================================
 *
 * [Vai trò]
 * Tạo MinioClient bean từ FileStorageProperties khi file storage được bật.
 * Config class chỉ dựng client, không upload/download object.
 *
 * [Giữ nguyên]
 * - Chỉ active khi APP_FILE_STORAGE_ENABLED=true.
 * - Không hardcode secret thật.
 * - Không upload/download trong config class.
 *
 * ==============================================================
 */
@Configuration
@ConditionalOnProperty(prefix = "app.file-storage", name = "enabled", havingValue = "true")
public class MinioClientConfig {
    @Bean
    MinioClient minioClient(FileStorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
