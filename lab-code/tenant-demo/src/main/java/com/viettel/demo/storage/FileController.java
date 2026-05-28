package com.viettel.demo.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/*
 * ==============================================================
 * FileController — REST API mỏng cho file storage mini-lab
 * ==============================================================
 *
 * [Endpoint dự kiến]
 * - POST   /api/files          upload multipart file.
 * - GET    /api/files/{fileId} download file theo metadata tenant-aware.
 * - DELETE /api/files/{fileId} delete/soft-delete file.
 *
 * [Lưu ý]
 * Controller chỉ là HTTP boundary mỏng. Không gọi MinIO trực tiếp,
 * không tự lấy tenantId từ request body, không nhận raw objectKey từ client.
 *
 * ==============================================================
 */
@RestController
@RequestMapping("/api/files")
@ConditionalOnProperty(prefix = "app.file-storage", name = "enabled", havingValue = "true")
public class FileController {

    private final FileStorageService service;

    public FileController(FileStorageService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(
            @PathVariable("fileId") String fileId
    ) {
        FileDownloadInfo file = service.download(fileId);
        Resource resource = new InputStreamResource(file.content());

        /*
         * Controller chỉ dựng HTTP response:
         * - metadata/content type đến từ PostgreSQL;
         * - binary stream đến từ MinIO qua service/gateway;
         * - không expose raw objectKey hoặc raw MinIO response ra client.
         */
        return ResponseEntity.ok()
                .contentType(resolveContentType(file.contentType()))
                .contentLength(file.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFilename())
                        .build()
                        .toString())
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable("fileId") String fileId
    ) {
        service.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(contentType);
    }
}
