package com.cadac.stone_inscription.file;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.exception.StoneInscriptionException;

class FileValidationServiceTests {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
    }

    @Test
    void validJpegFileIsAccepted() throws IOException {
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "application/octet-stream", jpegBytes);

        FileValidationService.ValidatedImage validatedImage = fileValidationService.validateImageFile(file);

        assertNotNull(validatedImage);
        assertEquals(".jpg", validatedImage.extension());
        assertEquals("image/jpeg", validatedImage.contentType());
        assertTrue(validatedImage.storedFileName().endsWith(".jpg"));
        assertEquals(jpegBytes.length, validatedImage.fileSize());
        assertArrayEquals(jpegBytes, validatedImage.bytes());
    }

    @Test
    void validPngFileIsAccepted() throws IOException {
        byte[] pngBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MultipartFile file = new MockMultipartFile("file", "photo.png", "application/octet-stream", pngBytes);

        FileValidationService.ValidatedImage validatedImage = fileValidationService.validateImageFile(file);

        assertNotNull(validatedImage);
        assertEquals(".png", validatedImage.extension());
        assertEquals("image/png", validatedImage.contentType());
        assertTrue(validatedImage.storedFileName().endsWith(".png"));
    }

    @Test
    void fileWithDoubleExtensionIsRejected() {
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MultipartFile file = new MockMultipartFile("file", "test.php.jpg", "application/octet-stream", jpegBytes);

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> fileValidationService.validateImageFile(file));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Double extension detected"));
    }

    @Test
    void disguisedNonImageFileIsRejected() {
        byte[] fakeBytes = "<?php echo 'hello'; ?>".getBytes();
        MultipartFile file = new MockMultipartFile("file", "image.jpg", "application/octet-stream", fakeBytes);

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> fileValidationService.validateImageFile(file));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Invalid image file"));
    }

    @Test
    void oversizedFileIsRejected() {
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        MultipartFile file = new MockMultipartFile("file", "large.jpg", "application/octet-stream", jpegBytes) {
            @Override
            public long getSize() {
                return FileValidationService.MAX_IMAGE_SIZE_BYTES + 1;
            }
        };

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> fileValidationService.validateImageFile(file));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("75 MB"));
    }

    @Test
    void zeroByteFileIsRejected() {
        MultipartFile file = new MockMultipartFile("file", "empty.png", "application/octet-stream", new byte[0]);

        StoneInscriptionException exception = assertThrows(StoneInscriptionException.class,
                () -> fileValidationService.validateImageFile(file));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getHttpStatus());
        assertTrue(exception.getMessage().contains("Image file is required"));
    }
}
