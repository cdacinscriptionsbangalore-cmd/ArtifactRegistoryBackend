package com.cadac.stone_inscription.minio.client;

import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

@Component
public class SdkMinioOperations implements MinioOperations {

    private final MinioClient minioClient;

    public SdkMinioOperations(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public boolean bucketExists(BucketExistsArgs args) throws Exception {
        return minioClient.bucketExists(args);
    }

    @Override
    public void makeBucket(MakeBucketArgs args) throws Exception {
        minioClient.makeBucket(args);
    }

    @Override
    public ObjectWriteResponse putObject(PutObjectArgs args) throws Exception {
        return minioClient.putObject(args);
    }

    @Override
    public GetObjectResponse getObject(GetObjectArgs args) throws Exception {
        return minioClient.getObject(args);
    }

    @Override
    public void removeObject(RemoveObjectArgs args) throws Exception {
        minioClient.removeObject(args);
    }
}
