package com.viettel.files.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<FileMetadata> findByTenantIdAndFileId(Long tenantId, String fileId);
}
