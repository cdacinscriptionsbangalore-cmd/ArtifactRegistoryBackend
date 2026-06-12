package com.cadac.stone_inscription.minio.client;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

public interface MinioOperations {

    boolean bucketExists(BucketExistsArgs args) throws Exception;

    void makeBucket(MakeBucketArgs args) throws Exception;

    ObjectWriteResponse putObject(PutObjectArgs args) throws Exception;

    GetObjectResponse getObject(GetObjectArgs args) throws Exception;

    void removeObject(RemoveObjectArgs args) throws Exception;
}
