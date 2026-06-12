package com.cadac.stone_inscription.minio.model;

public record StoredObject(
        String bucket,
        String objectName,
        String etag,
        String versionId) {
}
