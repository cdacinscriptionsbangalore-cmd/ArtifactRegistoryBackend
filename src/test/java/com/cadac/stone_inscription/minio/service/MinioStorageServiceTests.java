package com.cadac.stone_inscription.minio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class MinioStorageServiceTests {

    private FakeMinioOperations minioOperations;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setBucket("test-bucket");
        minioOperations = new FakeMinioOperations();
        storageService = new MinioStorageService(minioOperations, properties);
    }

    @Test
    void createsBucketWhenItDoesNotExist() throws Exception {
        minioOperations.bucketExists = false;

        storageService.ensureBucketExists();

        assertEquals(1, minioOperations.makeBucketCalls);
    }

    @Test
    void doesNotCreateBucketWhenItAlreadyExists() throws Exception {
        minioOperations.bucketExists = true;

        storageService.ensureBucketExists();

        assertEquals(0, minioOperations.makeBucketCalls);
    }

    @Test
    void returnsStoredObjectDetailsAfterUpload() throws Exception {
        minioOperations.putObjectResponse = new ObjectWriteResponse(
                null, "test-bucket", null, "images/photo.jpg", "etag-value", "version-1");

        byte[] content = { 1, 2, 3 };
        StoredObject storedObject = storageService.putObject(
                "images/photo.jpg",
                new ByteArrayInputStream(content),
                content.length,
                "image/jpeg");

        assertEquals("test-bucket", storedObject.bucket());
        assertEquals("images/photo.jpg", storedObject.objectName());
        assertEquals("etag-value", storedObject.etag());
        assertEquals("version-1", storedObject.versionId());
    }

    @Test
    void wrapsClientErrorsInStorageException() throws Exception {
        minioOperations.failure = new IllegalStateException("connection failed");

        assertThrows(MinioStorageException.class, storageService::ensureBucketExists);
    }

    @Test
    void rejectsBlankObjectNamesBeforeCallingMinio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.putObject(" ", new ByteArrayInputStream(new byte[0]), 0, null));
    }

    private static class FakeMinioOperations implements MinioOperations {

        private boolean bucketExists;
        private int makeBucketCalls;
        private ObjectWriteResponse putObjectResponse;
        private RuntimeException failure;

        @Override
        public boolean bucketExists(BucketExistsArgs args) {
            throwIfFailed();
            return bucketExists;
        }

        @Override
        public void makeBucket(MakeBucketArgs args) {
            throwIfFailed();
            makeBucketCalls++;
        }

        @Override
        public ObjectWriteResponse putObject(PutObjectArgs args) {
            throwIfFailed();
            return putObjectResponse;
        }

        @Override
        public GetObjectResponse getObject(GetObjectArgs args) {
            throwIfFailed();
            return null;
        }

        @Override
        public void removeObject(RemoveObjectArgs args) {
            throwIfFailed();
        }

        private void throwIfFailed() {
            if (failure != null) {
                throw failure;
            }
        }
    }
}
