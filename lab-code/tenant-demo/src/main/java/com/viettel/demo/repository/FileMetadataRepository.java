package com.viettel.demo.repository;

/*
 * ==============================================================
 * FileMetadata Repository
 * ==============================================================
 *
 * [Mục tiêu]
 * Repository cho entity FileMetadata, query tenant-aware rõ ràng.
 *
 * ==============================================================
 */

import com.viettel.demo.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByTenantIdAndFileId(Long tenantId, String fileId);
}

