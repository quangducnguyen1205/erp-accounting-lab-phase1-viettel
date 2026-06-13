package com.viettel.demo.storage;

import com.viettel.common.security.TenantContext;
import com.viettel.demo.entity.FileMetadata;
import com.viettel.demo.repository.FileMetadataRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/*
 * ==============================================================
 * FileStorageService — use case file storage tenant-aware
 * ==============================================================
 *
 * [Vai trò]
 * Service này sẽ là nơi nối:
 * - TenantContext;
 * - metadata PostgreSQL;
 * - object key generation;
 * - FileStorageGateway gọi MinIO.
 *
 * [Luồng chính]
 * 1. Lấy tenantId từ TenantContext.
 * 2. Sinh fileId + objectKey tenant-aware ở backend.
 * 3. Upload binary stream lên MinIO qua gateway.
 * 4. Lưu metadata tenant-aware vào PostgreSQL.
 * 5. Download/delete luôn tìm metadata bằng tenantId + fileId.
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

    private static final String DEFAULT_FILENAME = "file";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileStorageGateway gateway;
    private final FileMetadataRepository metadataRepository;

    public FileStorageService(
            FileStorageGateway gateway,
            FileMetadataRepository metadataRepository) {
        this.gateway = gateway;
        this.metadataRepository = metadataRepository;
    }

    public FileUploadResponse upload(MultipartFile file) {
        Long tenantId = getTenantId();
        String fileId = UUID.randomUUID().toString();
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String contentType = resolveContentType(file.getContentType());
        String objectKey = generateObjectKey(tenantId, fileId, originalFilename);
        long sizeBytes = file.getSize();

        Map<String, String> metadata = Map.of(
                "tenant-id", tenantId.toString(),
                "original-filename", originalFilename,
                "content-type", contentType
        );

        try (InputStream inputStream = file.getInputStream()) {
            gateway.putObject(
                    objectKey,
                    inputStream,
                    sizeBytes,
                    contentType,
                    metadata
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file input stream", e);
        }

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileId(fileId);
        fileMetadata.setObjectKey(objectKey);
        fileMetadata.setOriginalFilename(originalFilename);
        fileMetadata.setContentType(contentType);
        fileMetadata.setSizeBytes(sizeBytes);

        try {
            metadataRepository.save(fileMetadata);
        } catch (RuntimeException e) {
            cleanupUploadedObject(objectKey, e);
            throw e;
        }

        return new FileUploadResponse(fileId, originalFilename, contentType, sizeBytes);
    }

    public FileDownloadInfo download(String fileId) {
        FileMetadata fileMetadata = findTenantFile(fileId);
        return new FileDownloadInfo(
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getSizeBytes(),
                gateway.getObject(fileMetadata.getObjectKey())
        );
    }

    public void delete(String fileId) {
        FileMetadata fileMetadata = findTenantFile(fileId);
        gateway.removeObject(fileMetadata.getObjectKey());
        metadataRepository.delete(fileMetadata);
    }

    private FileMetadata findTenantFile(String fileId) {
        return metadataRepository.findByTenantIdAndFileId(
                getTenantId(),
                fileId
        ).orElseThrow(() -> new FileStorageNotFoundException("File not found"));
    }

    private String generateObjectKey(Long tenantId, String fileId, String safeFilename) {
        return String.format("tenant/%d/files/%s/%s", tenantId, fileId, safeFilename);
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return DEFAULT_FILENAME;
        }

        String filename = originalFilename.replace("\\", "/");
        int slashIndex = filename.lastIndexOf('/');
        if (slashIndex >= 0) {
            filename = filename.substring(slashIndex + 1);
        }

        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (filename.isBlank() || ".".equals(filename) || "..".equals(filename)) {
            return DEFAULT_FILENAME;
        }

        return filename.length() > 120 ? filename.substring(0, 120) : filename;
    }

    private String resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    private void cleanupUploadedObject(String objectKey, RuntimeException originalException) {
        try {
            gateway.removeObject(objectKey);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set");
        }
        return tenantId;
    }
}
