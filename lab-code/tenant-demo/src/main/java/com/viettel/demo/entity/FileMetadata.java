package com.viettel.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/*
 * ==============================================================
 * FileMetadata Entity — metadata tenant-aware cho file storage
 * ==============================================================
 *
 * [Mục tiêu]
 * Mapping với bảng file_metadata để lưu thông tin file upload.
 * Entity này kế thừa TenantAwareEntity → tự có tenant_id.
 *
 * [Mapping]
 * - file_id: id business dùng để truy cập file.
 * - object_key: key thực tế lưu trong MinIO.
 * - created_at: DEFAULT now() từ DB.
 *
 * ==============================================================
 */
@Entity
@Table(name = "file_metadata",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_file_metadata_tenant_file",
                columnNames = {"tenant_id", "file_id"}))
public class FileMetadata extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false, length = 64)
    private String fileId;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

