package com.cadac.stone_inscription.minio.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cadac.stone_inscription.minio.client.MinioOperations;
import com.cadac.stone_inscription.minio.config.MinioProperties;
import com.cadac.stone_inscription.minio.exception.MinioStorageException;
import com.cadac.stone_inscription.minio.model.StoredObject;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Service
public class MinioStorageService {

    private static final long DEFAULT_PART_SIZE = -1;

    private final MinioOperations minioOperations;
    private final MinioProperties properties;

    public MinioStorageService(MinioOperations minioOperations, MinioProperties properties) {
        this.minioOperations = minioOperations;
        this.properties = properties;
    }

    public void ensureBucketExists() {
        try {
            BucketExistsArgs existsArgs = BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build();

            if (!minioOperations.bucketExists(existsArgs)) {
                MakeBucketArgs.Builder makeBucket = MakeBucketArgs.builder()
                        .bucket(properties.getBucket());

                if (StringUtils.hasText(properties.getRegion())) {
                    makeBucket.region(properties.getRegion());
                }

                minioOperations.makeBucket(makeBucket.build());
            }
        } catch (Exception exception) {
            throw storageFailure("ensure bucket exists", properties.getBucket(), exception);
        }
    }

    public StoredObject putObject(
            String objectName,
            InputStream inputStream,
            long objectSize,
            String contentType) {
        validateObjectName(objectName);
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (objectSize < 0) {
            throw new IllegalArgumentException("objectSize must not be negative");
        }

        try {
            PutObjectArgs.Builder request = PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .stream(inputStream, objectSize, DEFAULT_PART_SIZE);

            if (StringUtils.hasText(contentType)) {
                request.contentType(contentType);
            }

            ObjectWriteResponse response = minioOperations.putObject(request.build());
            return new StoredObject(
                    response.bucket(),
                    response.object(),
                    response.etag(),
                    response.versionId());
        } catch (Exception exception) {
            throw storageFailure("store object", objectName, exception);
        }
    }

    public GetObjectResponse getObject(String objectName) {
        validateObjectName(objectName);

        try {
            return minioOperations.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("retrieve object", objectName, exception);
        }
    }

    public void removeObject(String objectName) {
        validateObjectName(objectName);

        try {
            minioOperations.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("remove object", objectName, exception);
        }
    }

    private void validateObjectName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            throw new IllegalArgumentException("objectName must not be blank");
        }
    }

    private MinioStorageException storageFailure(String operation, String target, Exception cause) {
        return new MinioStorageException(
                "Failed to " + operation + " '" + target + "' in MinIO bucket '"
                        + properties.getBucket() + "'",
                cause);
    }
}
