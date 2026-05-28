package com.viettel.demo.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * FileController — skeleton REST API cho file storage mini-lab
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
    public ResponseEntity<Void> download(
            @PathVariable("fileId") String fileId
    ) {
        /*
         * TODO:
         * - service.download(fileId)
         * - set Content-Type/Content-Disposition
         * - stream InputStream về response.
         */
        service.download(fileId);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @PathVariable("fileId") String fileId
    ) {
        service.delete(fileId);
        return ResponseEntity.noContent().build();
    }
}
