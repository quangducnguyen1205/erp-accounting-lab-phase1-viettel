package com.viettel.files.file;

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

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService service;

    public FileController(FileStorageService service) {
        this.service = service;
    }

    @GetMapping
    public List<FileMetadataResponse> list() {
        return service.listCurrentTenantFiles();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        FileDownloadInfo file = service.download(fileId);
        Resource resource = new InputStreamResource(file.content());

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
    public ResponseEntity<Void> delete(@PathVariable String fileId) {
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
