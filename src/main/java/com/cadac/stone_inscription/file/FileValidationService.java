package com.cadac.stone_inscription.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.exception.StoneInscriptionException;

@Service
public class FileValidationService {

    public static final long MAX_IMAGE_SIZE_BYTES = 75L * 1024 * 1024;

    private static final Map<String, byte[]> ALLOWED_MAGIC_BYTES = Map.of(
            "JPEG", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },
            "PNG", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 },
            "WEBP", new byte[] { 0x52, 0x49, 0x46, 0x46 },
            "GIF", new byte[] { 0x47, 0x49, 0x46, 0x38 });

    private static final Map<String, String> MAGIC_CONTENT_TYPE = Map.of(
            "JPEG", "image/jpeg",
            "PNG", "image/png",
            "WEBP", "image/webp",
            "GIF", "image/gif");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif");

    public record ValidatedImage(
            byte[] bytes,
            String storedFileName,
            String contentType,
            long fileSize,
            String extension) {
    }

    public ValidatedImage validateImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new StoneInscriptionException("Image file is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (file.getSize() <= 0) {
            throw new StoneInscriptionException("Uploaded file is empty", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new StoneInscriptionException("Each image size should be less than or equal to 75 MB",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String contentType = validateMagicBytes(file);
        byte[] bytes = file.getBytes();
        String storedFileName = UUID.randomUUID().toString() + extension;

        return new ValidatedImage(bytes, storedFileName, contentType, bytes.length, extension);
    }

    public String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new StoneInscriptionException("Filename is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            throw new StoneInscriptionException("No file extension", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String beforeExt = filename.substring(0, lastDot);
        if (beforeExt.contains(".")) {
            throw new StoneInscriptionException("Double extension detected", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String ext = filename.substring(lastDot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new StoneInscriptionException("Extension not allowed: " + ext, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return ext;
    }

    public long getMaxImageSizeBytes() {
        return MAX_IMAGE_SIZE_BYTES;
    }

    private String validateMagicBytes(MultipartFile file) throws IOException {
        byte[] header = new byte[8];
        int bytesRead;

        try (InputStream inputStream = file.getInputStream()) {
            bytesRead = inputStream.read(header);
        }

        if (bytesRead < 4) {
            throw new StoneInscriptionException("Uploaded file is too small to be a valid image",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return ALLOWED_MAGIC_BYTES.entrySet().stream()
                .filter(entry -> startsWith(header, entry.getValue()))
                .findFirst()
                .map(entry -> MAGIC_CONTENT_TYPE.get(entry.getKey()))
                .orElseThrow(() -> new StoneInscriptionException("Invalid image file", HttpStatus.UNPROCESSABLE_ENTITY));
    }

    private boolean startsWith(byte[] header, byte[] magicBytes) {
        if (header.length < magicBytes.length) {
            return false;
        }

        for (int i = 0; i < magicBytes.length; i++) {
            if (header[i] != magicBytes[i]) {
                return false;
            }
        }

        return true;
    }
}
