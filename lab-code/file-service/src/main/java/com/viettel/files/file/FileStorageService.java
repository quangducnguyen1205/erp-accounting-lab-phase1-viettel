package com.viettel.files.file;

import com.viettel.common.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final String DEFAULT_FILENAME = "file";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileStorageGateway gateway;
    private final FileMetadataRepository metadataRepository;

    public FileStorageService(FileStorageGateway gateway, FileMetadataRepository metadataRepository) {
        this.gateway = gateway;
        this.metadataRepository = metadataRepository;
    }

    @Transactional(readOnly = true)
    public List<FileMetadataResponse> listCurrentTenantFiles() {
        Long tenantId = currentTenantId();
        return metadataRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(FileMetadataResponse::from)
                .toList();
    }

    @Transactional
    public FileUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        Long tenantId = currentTenantId();
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
            gateway.putObject(objectKey, inputStream, sizeBytes, contentType, metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file input stream", e);
        }

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setTenantId(tenantId);
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

        log.info(
                "Uploaded file fileId={}, tenantId={}, filename={}, sizeBytes={}",
                fileId,
                tenantId,
                originalFilename,
                sizeBytes
        );

        return new FileUploadResponse(fileId, originalFilename, contentType, sizeBytes);
    }

    @Transactional(readOnly = true)
    public FileDownloadInfo download(String fileId) {
        FileMetadata fileMetadata = findTenantFile(fileId);
        log.info("Downloading file fileId={}, tenantId={}", fileMetadata.getFileId(), fileMetadata.getTenantId());
        return new FileDownloadInfo(
                fileMetadata.getOriginalFilename(),
                fileMetadata.getContentType(),
                fileMetadata.getSizeBytes(),
                gateway.getObject(fileMetadata.getObjectKey())
        );
    }

    @Transactional
    public void delete(String fileId) {
        FileMetadata fileMetadata = findTenantFile(fileId);
        gateway.removeObject(fileMetadata.getObjectKey());
        metadataRepository.delete(fileMetadata);
        log.info("Deleted file fileId={}, tenantId={}", fileMetadata.getFileId(), fileMetadata.getTenantId());
    }

    private FileMetadata findTenantFile(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileId cannot be blank");
        }

        return metadataRepository.findByTenantIdAndFileId(currentTenantId(), fileId.trim())
                .orElseThrow(() -> new FileStorageNotFoundException("File not found"));
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

    private Long currentTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context is missing");
        }
        return tenantId;
    }
}
