package com.viettel.demo.storage;

import io.minio.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.file-storage", name = "enabled", havingValue = "true")
public class MinioFileStorageGateway implements FileStorageGateway {
    private final MinioClient minioClient;
    private final FileStorageProperties properties;

    public MinioFileStorageGateway(MinioClient minioClient, FileStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public StoredObjectInfo putObject(
            String objectKey,
            InputStream inputStream,
            long sizeBytes,
            String contentType,
            Map<String, String> metadata) {
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .stream(inputStream, sizeBytes, -1)
                .contentType(contentType)
                .userMetadata(metadata)
                .build();
        try {
            ObjectWriteResponse response = minioClient.putObject(args);
            return new StoredObjectInfo(
                    properties.getBucket(),
                    objectKey,
                    response.etag(),
                    contentType,
                    sizeBytes
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to put object to MinIO", e);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build();
        try {
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object from MinIO", e);
        }
    }

    @Override
    public StoredObjectInfo statObject(String objectKey) {
        StatObjectArgs args = StatObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build();
        try {
            StatObjectResponse stat = minioClient.statObject(args);
            return new StoredObjectInfo(
                    properties.getBucket(),
                    objectKey,
                    stat.etag(),
                    stat.contentType(),
                    stat.size()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to stat object from MinIO", e);
        }
    }

    @Override
    public void removeObject(String objectKey) {
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(properties.getBucket())
                .object(objectKey)
                .build();
        try {
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove object from MinIO", e);
        }
    }
}
