package com.cadac.stone_inscription.minio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.cadac.stone_inscription.entity.ImagesData;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.UserImage;
import com.cadac.stone_inscription.file.FileValidationService;
import com.cadac.stone_inscription.minio.config.MinioProperties;
import com.cadac.stone_inscription.minio.model.StoredObject;
import com.cadac.stone_inscription.minio.service.MinioStorageService;
import com.cadac.stone_inscription.post.service.PostServiceImp;
import com.cadac.stone_inscription.post.util.ImageMetadataGeolocationWithPhash.ImageMetaAndInfo;
import com.cadac.stone_inscription.repository.ImagesDataRepo;
import com.cadac.stone_inscription.repository.UserImageRepo;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.user.service.UserServiceImpl;

import dev.brachtendorf.jimagehash.hash.Hash;

class DatabaseFileStorageMigrationTests {

    @Test
    void postImageStoresBytesInMinioAndOnlyReferenceInMongo() {
        TrackingStorage storage = new TrackingStorage();
        SavedValue<ImagesData> savedImage = new SavedValue<>();
        ImagesDataRepo imagesDataRepo = repositoryProxy(ImagesDataRepo.class, (method, args) -> {
            if (method.equals("save")) {
                ImagesData image = (ImagesData) args[0];
                image.setId("post-image-id");
                savedImage.value = image;
                return image;
            }
            return defaultValue(method);
        });

        PostServiceImp service = new PostServiceImp();
        ReflectionTestUtils.setField(service, "imagesDataRepo", imagesDataRepo);
        ReflectionTestUtils.setField(service, "minioStorageService", storage);

        ObjectId postId = new ObjectId();
        byte[] bytes = { 1, 2, 3, 4 };
        ImageMetaAndInfo image = ImageMetaAndInfo.builder()
                .file(bytes)
                .fileName("generated.jpg")
                .fileSize((long) bytes.length)
                .contentType("image/jpeg")
                .pHash(new Hash(BigInteger.TEN, 64, 1))
                .build();

        List<String> imageIds = ReflectionTestUtils.invokeMethod(service, "saveImages", postId, List.of(image));

        assertEquals(List.of("post-image-id"), imageIds);
        assertArrayEquals(bytes, storage.storedBytes);
        assertEquals("posts/" + postId.toHexString() + "/generated.jpg", storage.storedObjectName);
        assertEquals(storage.storedObjectName, savedImage.value.getObjectName());
        assertNotNull(savedImage.value.getMetadata());
    }

    @Test
    void profileImageStoresBytesInMinioAndOnlyReferenceInMongo() {
        TrackingStorage storage = new TrackingStorage();
        ObjectId userId = new ObjectId();
        User user = User.builder().id(userId).email("user@example.com").name("User").build();
        SavedValue<UserImage> savedImage = new SavedValue<>();

        UserRepository userRepository = repositoryProxy(UserRepository.class, (method, args) -> {
            if (method.equals("findByEmail")) {
                return user;
            }
            if (method.equals("save")) {
                return args[0];
            }
            return defaultValue(method);
        });
        UserImageRepo userImageRepo = repositoryProxy(UserImageRepo.class, (method, args) -> {
            if (method.equals("findByUserIdAndImageType")) {
                return Optional.empty();
            }
            if (method.equals("save")) {
                UserImage image = (UserImage) args[0];
                image.setId("user-image-id");
                savedImage.value = image;
                return image;
            }
            return defaultValue(method);
        });

        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "userImageRepo", userImageRepo);
        ReflectionTestUtils.setField(service, "fileValidationService", new FileValidationService());
        ReflectionTestUtils.setField(service, "minioStorageService", storage);
        ReflectionTestUtils.setField(service, "backendUrl", "http://backend");

        byte[] jpeg = { (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0, 1 };
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", jpeg);

        assertEquals(HttpStatus.OK, service.uploadProfileImage("user@example.com", file).getStatusCode());
        assertArrayEquals(jpeg, storage.storedBytes);
        assertTrue(storage.storedObjectName.startsWith("users/" + userId.toHexString() + "/profile/"));
        assertEquals(storage.storedObjectName, savedImage.value.getObjectName());
        assertEquals("http://backend/user/public/images/user-image-id", user.getProfileImage());
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> type, RepositoryInvocation invocation) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] { type },
                (proxy, method, args) -> invocation.invoke(method.getName(), args == null ? new Object[0] : args));
    }

    private Object defaultValue(String method) {
        if (method.startsWith("find")) {
            return Optional.empty();
        }
        return null;
    }

    @FunctionalInterface
    private interface RepositoryInvocation {
        Object invoke(String method, Object[] args);
    }

    private static class SavedValue<T> {
        private T value;
    }

    private static class TrackingStorage extends MinioStorageService {

        private String storedObjectName;
        private byte[] storedBytes;

        TrackingStorage() {
            super(null, properties());
        }

        @Override
        public StoredObject putObject(
                String objectName,
                InputStream inputStream,
                long objectSize,
                String contentType) {
            try {
                storedObjectName = objectName;
                storedBytes = inputStream.readAllBytes();
                return new StoredObject("test-bucket", objectName, "etag", null);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public void removeObject(String objectName) {
        }

        private static MinioProperties properties() {
            MinioProperties properties = new MinioProperties();
            properties.setBucket("test-bucket");
            return properties;
        }
    }
}
