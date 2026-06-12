package com.cadac.stone_inscription.minio.exception;

public class MinioStorageException extends RuntimeException {

    public MinioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
