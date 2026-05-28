package com.viettel.demo.storage;

import com.viettel.demo.context.TenantContext;
import com.viettel.demo.entity.FileMetadata;
import com.viettel.demo.repository.FileMetadataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/*
 * ==============================================================
 * FileStorageService — skeleton use case file storage tenant-aware
 * ==============================================================
 *
 * [Vai trò]
 * Service này sẽ là nơi nối:
 * - TenantContext;
 * - metadata PostgreSQL;
 * - object key generation;
 * - FileStorageGateway gọi MinIO.
 *
 * [Khi tự implement]
 * 1. Lấy tenantId từ TenantContext.
 * 2. Validate file size/content type cơ bản.
 * 3. Sinh fileId + objectKey tenant-aware.
 * 4. Gọi gateway.putObject(...).
 * 5. Lưu metadata tenant-aware vào PostgreSQL.
 * 6. Download phải tìm metadata bằng tenantId + fileId.
 *
 * [Không làm]
 * - Không nhận tenantId từ request body.
 * - Không nhận raw objectKey từ client.
 * - Không biến MinIO thành source of truth nghiệp vụ.
 *
 * ==============================================================
 */
@Service
@ConditionalOnProperty(prefix = "app.file-storage", name = "enabled", havingValue = "true")
public class FileStorageService {

    private final FileStorageGateway gateway;
    private final FileMetadataRepository metadataRepository;

    public FileStorageService(
            FileStorageGateway gateway,
            FileMetadataRepository metadataRepository) {
        this.gateway = gateway;
        this.metadataRepository = metadataRepository;
    }

    public FileUploadResponse upload(MultipartFile file) {
        String fileId = "file-" + System.currentTimeMillis();
        String objectKey = generateObjectKey(getTenantId(), fileId);
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (originalFilename == null || contentType == null) {
            throw new IllegalArgumentException("Original filename and content type are required");
        }
        Map<String, String> metadata = Map.of(
                "originalFilename", originalFilename,
                "contentType", contentType
        );
        long sizeBytes = file.getSize();
        try {
            gateway.putObject(
                    objectKey,
                    file.getInputStream(),
                    sizeBytes,
                    file.getContentType(),
                    metadata
            );
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFileId(fileId);
            fileMetadata.setObjectKey(objectKey);
            fileMetadata.setOriginalFilename(file.getOriginalFilename());
            fileMetadata.setContentType(file.getContentType());
            fileMetadata.setSizeBytes(sizeBytes);
            metadataRepository.save(fileMetadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file input stream", e);
        }
        return new FileUploadResponse(fileId, originalFilename, contentType, sizeBytes);
    }

    public FileDownloadInfo download(String fileId) {
        FileMetadata fileMetadata = metadataRepository.findByTenantIdAndFileId(
                getTenantId(),
                fileId
        ).orElseThrow(() -> new IllegalArgumentException("File not found for tenant"));
        return new FileDownloadInfo(
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getSizeBytes(),
                gateway.getObject(fileMetadata.getObjectKey())
        );
    }

    public void delete(String fileId) {
        gateway.removeObject(generateObjectKey(getTenantId(), fileId));
    }

    private String generateObjectKey(Long tenantId, String fileId) {
        return String.format("%s/%s", tenantId, fileId);
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set");
        }
        return tenantId;
    }
}
