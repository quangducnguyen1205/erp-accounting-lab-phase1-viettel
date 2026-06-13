package com.viettel.files.file;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

@Component
public class MinioFileStorageGateway implements FileStorageGateway {

    private final MinioClient minioClient;
    private final FileStorageProperties properties;
    private volatile boolean bucketReady;

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
        ensureBucketExists();
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
            GetObjectResponse response = minioClient.getObject(args);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object from MinIO", e);
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

    private void ensureBucketExists() {
        if (bucketReady) {
            return;
        }

        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
            }
            bucketReady = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare MinIO bucket", e);
        }
    }
}
