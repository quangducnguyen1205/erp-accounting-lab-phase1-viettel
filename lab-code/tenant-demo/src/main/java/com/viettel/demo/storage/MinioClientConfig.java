package com.viettel.demo.storage;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * ==============================================================
 * MinioClientConfig — skeleton cấu hình MinIO Java SDK
 * ==============================================================
 *
 * [Hiện tại]
 * File này chỉ là skeleton compile-safe. Chưa tạo MinioClient bean để
 * tránh kéo dependency/runtime storage khi bạn chưa tự code mini-lab.
 *
 * [Khi tự implement]
 * 1. Thêm dependency MinIO Java SDK vào pom.xml.
 * 2. Import io.minio.MinioClient.
 * 3. Tạo @Bean MinioClient từ FileStorageProperties:
 *
 *    MinioClient.builder()
 *        .endpoint(properties.getEndpoint())
 *        .credentials(properties.getAccessKey(), properties.getSecretKey())
 *        .build()
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
