package com.viettel.demo.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * ==============================================================
 * FileStorageProperties — cấu hình MinIO/file storage mini-lab
 * ==============================================================
 *
 * [Mục tiêu]
 * Gom config file storage vào một nơi rõ ràng:
 * - bật/tắt mini-lab;
 * - endpoint MinIO;
 * - access key / secret key local;
 * - bucket mặc định;
 * - region.
 *
 * [Lưu ý học tập]
 * - File storage tắt mặc định để app-test không phụ thuộc MinIO.
 * - Dev credentials trong .env.example chỉ dùng local lab.
 * - Khi tự code thật, MinioClientConfig sẽ đọc class này để tạo client.
 *
 * ==============================================================
 */
@Component
@ConfigurationProperties(prefix = "app.file-storage")
public class FileStorageProperties {

    private boolean enabled = false;
    private String endpoint = "http://localhost:19000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "tenant-demo-files";
    private String region = "us-east-1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
