package com.cadac.stone_inscription.post.service;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.entity.ImagesData;
import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.cadac.stone_inscription.post.dto.InscriptionPostDto;
import com.cadac.stone_inscription.post.dto.PublicPostUserDescriptionDto;
import com.cadac.stone_inscription.post.mapper.PostMapper;
import com.cadac.stone_inscription.post.mapper.PublicPostDescriptionMapper;
import com.cadac.stone_inscription.post.util.ImageMetadataGeolocationWithPhash;
import com.cadac.stone_inscription.post.util.ImageMetadataGeolocationWithPhash.ImageMetaAndInfo;
import com.cadac.stone_inscription.repository.ImagesDataRepo;
import com.cadac.stone_inscription.repository.InscriptionPostRepo;
import com.cadac.stone_inscription.repository.PublicPostDescriptionRepo;
import com.cadac.stone_inscription.repository.UserRepository;
import com.cadac.stone_inscription.util.UserResponse;

@Service
public class PostServiceImp implements PostService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InscriptionPostRepo inscriptionPostRepo;

    @Autowired
    private ImageMetadataGeolocationWithPhash metadataGeolocationWithPhash;

    @Autowired
    private ImagesDataRepo imagesDataRepo;

    @Autowired
    private PublicPostDescriptionRepo publicPostDescriptionRepo;

    @Value("${app.backend.url}")
    private String backendUrl;

    // Create Post + Process Images + Extract Location + Save Post + Update User
    // Stats

    @Override
    public ResponseEntity<?> addPostWithFile(InscriptionPostDto inscriptionPostDto, MultipartFile[] files,
            String usernameFromToken) {

        User user = userRepository.findByEmail(usernameFromToken);
        List<ImageMetaAndInfo> ls = validateAndExtractImages(files, user.getId(), Collections.emptySet(), true);

        // Below Line To use for Threshold similarty

        // Double similarty = ImagePhash.imagePHashComparing(ls.get(0).getPHash(),
        // ls.get(ls.size() - 1).getPHash());

        Optional<ImageMetadataGeolocationWithPhash.ImageMetaAndInfo> geoLocationInfoAndCordinates = ls.stream()
                .filter(el -> el.getGeocCordinates() != null)
                .findFirst();

        Optional<ImageMetadataGeolocationWithPhash.GeoCordinates> geoLocationCordinates = geoLocationInfoAndCordinates
                .isPresent()
                        ? Optional
                                .of(geoLocationInfoAndCordinates.get().getGeocCordinates())
                        : Optional.empty();
        // Optional<ImageMetadataGeolocationWithPhash.GeoCordinates>
        // geoLocationCordinates = ls.stream()
        // .map(el -> el.getGeocCordinates())
        // .filter(Objects::nonNull)
        // .findFirst();

        updateUserImagesUploaded(user, ls.size());

        ObjectId postUserId = user.getId();

        InscriptionPost inscriptionPost = new InscriptionPost();

        if (inscriptionPostDto != null) {
            inscriptionPost = PostMapper.toEntity(inscriptionPostDto);
        }

        inscriptionPost.setUserId(postUserId);

        if (geoLocationCordinates.isPresent()) {

            inscriptionPost.getDescription().setGeolocation(InscriptionPost.GeoLocation.builder()
                    .lat(geoLocationCordinates.get().getLatitude())
                    .lon(geoLocationCordinates.get().getLongitude())
                    .city(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getCity())
                    .state(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getState())
                    .country(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getCountry())
                    .amenity(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getAmenity())
                    .road(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getRoad())
                    .neighbourhood(
                            geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getNeighbourhood())
                    .suburb(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getSuburb())
                    .cityDistrict(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getCityDistrict())
                    .stateDistrict(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress()
                            .getStateDistrict())
                    .iso3166Lvl4(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress()
                            .getIso3166Lvl4())
                    .postcode(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getPostcode())
                    .county(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress().getCounty())
                    .countryCode(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddress()
                            .getCountryCode())
                    .placeId(geoLocationInfoAndCordinates.get().getGeoApiResponse().getPlaceId())
                    .licence(geoLocationInfoAndCordinates.get().getGeoApiResponse().getLicence())
                    .osmType(geoLocationInfoAndCordinates.get().getGeoApiResponse().getOsmType())
                    .osmId(geoLocationInfoAndCordinates.get().getGeoApiResponse().getOsmId())
                    .clazz(geoLocationInfoAndCordinates.get().getGeoApiResponse().getClazz())
                    .type(geoLocationInfoAndCordinates.get().getGeoApiResponse().getType())
                    .placeRank(geoLocationInfoAndCordinates.get().getGeoApiResponse().getPlaceRank())
                    .importance(geoLocationInfoAndCordinates.get().getGeoApiResponse().getImportance())
                    .addressType(geoLocationInfoAndCordinates.get().getGeoApiResponse().getAddressType())
                    .name(geoLocationInfoAndCordinates.get().getGeoApiResponse().getName())
                    .displayName(geoLocationInfoAndCordinates.get().getGeoApiResponse().getDisplayName())
                    .boundingbox(geoLocationInfoAndCordinates.get().getGeoApiResponse().getBoundingbox())
                    .build());

        }

        inscriptionPost = inscriptionPostRepo.save(inscriptionPost);
        ObjectId postId = inscriptionPost.getId();

        List<String> imageId = saveImages(postId, ls);

        inscriptionPost
                .setImages(InscriptionPost.Images.builder().image(imageId).thumbnailImage(imageId.get(0)).build());

        if (inscriptionPostDto != null) {
            inscriptionPost.setVisiblity(inscriptionPostDto.getVisiblity());
        }

        inscriptionPostRepo.save(inscriptionPost);
        return UserResponse.responseHandler("Images Uploaded Sucessfully", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> getAllPost() {

        List<InscriptionPost> allPost = inscriptionPostRepo.findAll();

        allPost.forEach(elem -> {

            if (elem.getVisiblity() || elem.getVisiblity() == null) {
                elem.setUserName(userRepository.findById(elem.getUserId()).get().getName());
            }

            elem.getImages().setImage(elem.getImages().getImage().stream().map(
                    el -> backendUrl + "/post/public/images/" + el)
                    .toList());

            elem.getImages()
                    .setThumbnailImage(backendUrl + "/post/public/images/" + elem.getImages().getThumbnailImage());

        });

        return UserResponse.responseHandler("All Posts", HttpStatus.OK, allPost);
    }

    @Override
    public ResponseEntity<InputStreamResource> getImages(String id) {
        ImagesData image = imagesDataRepo.findById(id).orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMetadata().getContentType()))
                .body(new InputStreamResource(new ByteArrayInputStream(image.getImageData())));
    }

    @Override
    public ResponseEntity<?> getAllUserPost(String usernameFromToken) {
        ObjectId postUserId = userRepository.findByEmail(usernameFromToken).getId();
        List<InscriptionPost> userPost = inscriptionPostRepo.findByUserId(postUserId);

        userPost.forEach(elem -> {

            elem.getImages().setImage(elem.getImages().getImage().stream().map(
                    el -> backendUrl + "/post/public/images/" + el)
                    .toList());

            elem.getImages()
                    .setThumbnailImage(backendUrl + "/post/public/images/" + elem.getImages().getThumbnailImage());

        });

        return UserResponse.responseHandler("All Posts", HttpStatus.OK, userPost);
    }

    @Override
    public ResponseEntity<?> addPoastDiscription(String usernameFromToken, String postId, String discription) {

        User user = userRepository.findByEmail(usernameFromToken);

        if (inscriptionPostRepo.findById(new ObjectId(postId)).isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }

        publicPostDescriptionRepo.save(PublicPostDescription.builder().description(discription)
                .postId(new ObjectId(postId)).userId(user.getId())
                .userImageUrl(user.getProfileImage()).username(user.getName()).build());

        return UserResponse.responseHandler("Discription Added sucessfully", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> getPostDiscription(String postId) {

        return UserResponse.responseHandler("Fetch All  Discription for Post", HttpStatus.OK,
                publicPostDescriptionRepo.findByPostId(new ObjectId(postId)));
    }

    @Override
    public ResponseEntity<?> updatePostDiscription(String usernameFromToken, String postId, String discription) {
        User user = userRepository.findByEmail(usernameFromToken);
        Optional<PublicPostDescription> postDiscription = publicPostDescriptionRepo.findById(new ObjectId(postId));

        if (postDiscription.isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }

        if (!postDiscription.get().getUserId().equals(user.getId())) {
            throw new StoneInscriptionException("Unprocesable request Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        postDiscription.get().setDescription(discription);

        publicPostDescriptionRepo.save(postDiscription.get());

        return UserResponse.responseHandler("Updated Discription", HttpStatus.OK, true);

    }

    @Override
    public ResponseEntity<?> addRating(String usernameFromToken, String postId, Double rating) {

        User user = userRepository.findByEmail(usernameFromToken);
        Optional<InscriptionPost> inscrptionPost = inscriptionPostRepo.findById(new ObjectId(postId));

        if (inscrptionPost.isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }

        Optional<InscriptionPost.UsersRating> existingRating = inscrptionPost.get().getUserRating()
                .stream()
                .filter(r -> r.getUserId().equals(user.getId().toString()))
                .findFirst();

        if (existingRating.isPresent()) {
            existingRating.get().setRating(rating);

        } else {
            inscrptionPost.get().getUserRating()
                    .add(InscriptionPost.UsersRating.builder().rating(rating).userId(user.getId().toString()).build());
        }

        inscrptionPost.get().setRating(inscrptionPost.get().getUserRating()
                .stream()
                .mapToDouble(InscriptionPost.UsersRating::getRating)
                .average()
                .orElse(0.0));

        ;

        inscriptionPostRepo.save(inscrptionPost.get());
        return UserResponse.responseHandler("ratting added ", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> addVote(String usernameFromToken, String descriptionId) {
        User user = userRepository.findByEmail(usernameFromToken);
        Optional<PublicPostDescription> postDiscription = publicPostDescriptionRepo
                .findById(new ObjectId(descriptionId));

        User postUser = userRepository.findById(postDiscription.get().getUserId()).get();

        if (postDiscription.get().getUserVote().stream()
                .anyMatch(el -> el.getUserId().equals(user.getId().toString()))) {
            postDiscription.get().setUserVote(postDiscription.get().getUserVote().stream()
                    .filter(elm -> !elm.getUserId().equals(user.getId().toString())).toList());

            postUser.setUpvotesReceived(postUser.getUpvotesReceived() - 1);
            ;

        } else {
            postDiscription.get().getUserVote()
                    .add(PublicPostDescription.UserVote.builder().userId(user.getId().toString()).build());

            postUser.setUpvotesReceived(postUser.getUpvotesReceived() + 1);
        }
        userRepository.save(postUser);
        postDiscription.get().setUpvote(postDiscription.get().getUserVote().size());

        publicPostDescriptionRepo.save(postDiscription.get());
        return UserResponse.responseHandler("Vote updated", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> userProfile(String usernameFromToken) {
        return UserResponse.responseHandler("User Profile", HttpStatus.OK,
                userRepository.findByEmail(usernameFromToken));
    }

    @Override
    public ResponseEntity<?> postDelete(String usernameFromToken, String postId) {
        User user = userRepository.findByEmail(usernameFromToken);

        Optional<InscriptionPost> postDelete = inscriptionPostRepo.findById(new ObjectId(postId));

        if (postDelete.isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }
        if (!user.getId().toString().equals(postDelete.get().getUserId().toString())) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }
        postDelete.get().getImages().getImage().stream().forEach(elem -> {
            imagesDataRepo.deleteById(elem);
        });

        publicPostDescriptionRepo.deleteAllByPostId(postId);
        inscriptionPostRepo.deleteById(new ObjectId(postId));
        return UserResponse.responseHandler("post deleted", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> descriptionDelete(String usernameFromToken, String descriptionId) {
        User user = userRepository.findByEmail(usernameFromToken);
        Optional<PublicPostDescription> postDiscription = publicPostDescriptionRepo
                .findById(new ObjectId(descriptionId));

        if (postDiscription.isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }

        if (!postDiscription.get().getUserId().equals(user.getId())) {
            throw new StoneInscriptionException("Unprocesable request Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        publicPostDescriptionRepo.deleteById(new ObjectId(descriptionId));

        return UserResponse.responseHandler("description deleted", HttpStatus.OK, true);
    }

    // Main function for the updation of the image
    // edit post details
    // delete some images
    // upload new images
    // keep at least one image in the post

    @Override
    public ResponseEntity<?> updatePost(String usernameFromToken, InscriptionPostDto inscriptionPostDto,
            String postId, List<String> deletedImageIds, MultipartFile[] files) {

        InscriptionPost post = getOwnedPost(usernameFromToken, postId);
        List<String> existingImageIds = getExistingImageIds(post);
        List<String> imagesToDelete = validateDeletedImageIds(existingImageIds, deletedImageIds, false);
        Set<String> deletableImageIds = new HashSet<>(imagesToDelete);
        List<ImageMetaAndInfo> newImages = validateAndExtractImages(files, post.getUserId(), deletableImageIds, false);

        ensureMinimumImageCount(existingImageIds.size(), deletableImageIds.size(), newImages.size());

        if (inscriptionPostDto != null) {
            post.setDescription(PostMapper.toEntityDescription(inscriptionPostDto.getDescription()));
            post.setScript(inscriptionPostDto.getScript());
            post.setTopic(inscriptionPostDto.getTopic());
            post.setType(inscriptionPostDto.getType());
        }

        List<String> updatedImageIds = removeDeletedImageIds(existingImageIds, deletableImageIds);
        updatedImageIds.addAll(saveImages(post.getId(), newImages));

        updatePostImages(post, updatedImageIds, deletableImageIds);
        inscriptionPostRepo.save(post);
        deleteImagesByIds(deletableImageIds);
        return UserResponse.responseHandler("post Updated ", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> addImagesToPost(String usernameFromToken, String postId, MultipartFile[] files) {
        InscriptionPost post = getOwnedPost(usernameFromToken, postId);
        List<ImageMetaAndInfo> newImages = validateAndExtractImages(files, post.getUserId(), Collections.emptySet(),
                true);

        List<String> updatedImageIds = getExistingImageIds(post);
        updatedImageIds.addAll(saveImages(post.getId(), newImages));

        updatePostImages(post, updatedImageIds, Collections.emptySet());
        inscriptionPostRepo.save(post);
        updateUserImagesUploaded(usernameFromToken, newImages.size());

        return UserResponse.responseHandler("Images Added To Post Successfully", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> deleteImagesFromPost(String usernameFromToken, String postId,
            List<String> deletedImageIds) {
        InscriptionPost post = getOwnedPost(usernameFromToken, postId);
        List<String> existingImageIds = getExistingImageIds(post);
        List<String> imagesToDelete = validateDeletedImageIds(existingImageIds, deletedImageIds, true);
        Set<String> deletableImageIds = new HashSet<>(imagesToDelete);

        ensureMinimumImageCount(existingImageIds.size(), deletableImageIds.size(), 0);

        List<String> updatedImageIds = removeDeletedImageIds(existingImageIds, deletableImageIds);
        updatePostImages(post, updatedImageIds, deletableImageIds);

        inscriptionPostRepo.save(post);
        deleteImagesByIds(deletableImageIds);
        updateUserImagesUploaded(usernameFromToken, -deletableImageIds.size());

        return UserResponse.responseHandler("Images Deleted From Post Successfully", HttpStatus.OK, true);
    }

    @Override
    public ResponseEntity<?> getCommentByUser(String usernameFromToken) {
        User user = userRepository.findByEmail(usernameFromToken);

        List<PublicPostDescription> postDiscription = publicPostDescriptionRepo.findAllByUserId(user.getId());

        List<PublicPostUserDescriptionDto> ls = postDiscription.stream()
                .map(el -> {

                    PublicPostUserDescriptionDto ref = PublicPostDescriptionMapper.toDto(el);
                    ref.setPostImageUrl(inscriptionPostRepo.findById(el.getPostId())
                            .map(post -> post.getImages().getThumbnailImage())
                            .map(imageId -> backendUrl + "/post/public/images/" + imageId)
                            .orElse(null));

                    return ref;

                }).collect(Collectors.toList());

        return UserResponse.responseHandler("user all comment ", HttpStatus.OK, ls);
    }

    @Override
    public ResponseEntity<?> getDashboardCounts() {

        Map<String, Integer> counts = new HashMap<>();

        List<InscriptionPost> allPost = inscriptionPostRepo.findAll();

        counts.put("totalUsers", userRepository.findAll().size());
        counts.put("totalPosts", allPost.size());
        counts.put("totalGeoTaggedPosts",
                (int) allPost.stream().filter(el -> el.getDescription().getGeolocation() != null).count());
        counts.put("totalTranslations", 0);

        return UserResponse.responseHandler("Dashboard Counts", HttpStatus.OK, counts);
    }

    private List<ImageMetaAndInfo> validateAndExtractImages(MultipartFile[] files, ObjectId userId,
            Set<String> replaceableImageIds, boolean filesRequired) {
        if (files == null || files.length == 0) {
            if (filesRequired) {
                throw new StoneInscriptionException("No File Uploaded", HttpStatus.BAD_REQUEST);
            }

            return List.of();
        }

        List<ImageMetaAndInfo> ls = metadataGeolocationWithPhash.getGeoLocationWithIamgeMetaandInfo(files);

        if (ls.size() == 0) {
            throw new StoneInscriptionException("No Valid Image Found in the Request", HttpStatus.BAD_REQUEST);
        }
        // here is the logic where we find the count of image in databse if count is
        // more than 1 then it is duplicate image by same user
        if (ls.size() != ls.stream().map(ImageMetaAndInfo::getPHash).distinct().count()) {
            throw new StoneInscriptionException("Duplicate Image Uploaded", HttpStatus.BAD_REQUEST);
        }

        boolean imageAlreadyExists = ls.stream()
                .anyMatch(image -> isImageAlreadyUploadedByUser(userId,
                        image.getPHash().getHashValue().toString(), replaceableImageIds));

        if (imageAlreadyExists) {
            throw new StoneInscriptionException("Image Already Uploaded", HttpStatus.CONFLICT);
        }

        return ls;
    }

    private boolean isImageAlreadyUploadedByUser(ObjectId userId, String imageHashValue,
            Set<String> replaceableImageIds) {
        if (userId == null) {
            return false;
        }

        return imagesDataRepo.findAllByMetadata_ImageHashValue(imageHashValue).stream()
                .filter(existingImage -> !replaceableImageIds.contains(existingImage.getId()))
                .map(ImagesData::getPostId)
                .filter(Objects::nonNull)
                .map(inscriptionPostRepo::findById)
                .flatMap(Optional::stream)
                .anyMatch(post -> userId.equals(post.getUserId()));
    }

    private List<String> saveImages(ObjectId postId, List<ImageMetaAndInfo> images) {
        return images.stream().map(image -> imagesDataRepo.save(ImagesData.builder()
                .imageData(image.getFile())
                .postId(postId)
                .metadata(ImagesData.Metadata.builder()
                        .fileName(image.getFileName())
                        .fileSize(image.getFileSize())
                        .contentType(image.getContentType())
                        .imageHashValue(image.getPHash().getHashValue().toString())
                        .build())
                .build()).getId()).toList();
    }

    private InscriptionPost getOwnedPost(String usernameFromToken, String postId) {
        User user = userRepository.findByEmail(usernameFromToken);
        Optional<InscriptionPost> inscriptionPost = inscriptionPostRepo.findById(new ObjectId(postId));

        if (inscriptionPost.isEmpty()) {
            throw new StoneInscriptionException("Unprocesable request", HttpStatus.BAD_REQUEST);
        }

        if (!user.getId().equals(inscriptionPost.get().getUserId())) {
            throw new StoneInscriptionException("Forbidden", HttpStatus.FORBIDDEN);
        }

        return inscriptionPost.get();
    }

    private List<String> getExistingImageIds(InscriptionPost post) {
        if (post.getImages() == null || post.getImages().getImage() == null) {
            return new LinkedList<>();
        }

        return new LinkedList<>(post.getImages().getImage());
    }

    private List<String> validateDeletedImageIds(List<String> existingImageIds, List<String> deletedImageIds,
            boolean deletionRequired) {
        List<String> imagesToDelete = sanitizeImageIds(deletedImageIds);

        if (deletionRequired && imagesToDelete.isEmpty()) {
            throw new StoneInscriptionException("No image selected for deletion", HttpStatus.BAD_REQUEST);
        }

        if (!existingImageIds.containsAll(imagesToDelete)) {
            throw new StoneInscriptionException("Invalid image selected for deletion", HttpStatus.BAD_REQUEST);
        }

        return imagesToDelete;
    }

    private void ensureMinimumImageCount(int existingImageCount, int deletedImageCount, int newImageCount) {
        int finalImageCount = existingImageCount - deletedImageCount + newImageCount;

        if (finalImageCount < 1) {
            throw new StoneInscriptionException("Post should have at least one image", HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> removeDeletedImageIds(List<String> existingImageIds, Set<String> deletableImageIds) {
        return existingImageIds.stream()
                .filter(imageId -> !deletableImageIds.contains(imageId))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private void updatePostImages(InscriptionPost post, List<String> updatedImageIds, Set<String> removedImageIds) {
        if (post.getImages() == null) {
            post.setImages(InscriptionPost.Images.builder().build());
        }

        post.getImages().setImage(updatedImageIds);

        if (post.getImages().getThumbnailImage() == null
                || removedImageIds.contains(post.getImages().getThumbnailImage())) {
            post.getImages().setThumbnailImage(updatedImageIds.get(0));
        }
    }

    private void deleteImagesByIds(Set<String> imageIds) {
        imageIds.forEach(imagesDataRepo::deleteById);
    }

    private void updateUserImagesUploaded(String usernameFromToken, int delta) {
        if (delta == 0) {
            return;
        }

        User user = userRepository.findByEmail(usernameFromToken);
        updateUserImagesUploaded(user, delta);
    }

    private void updateUserImagesUploaded(User user, int delta) {
        if (user == null || delta == 0) {
            return;
        }

        int currentImagesUploaded = user.getImagesUploaded() == null ? 0 : user.getImagesUploaded();
        user.setImagesUploaded(Math.max(0, currentImagesUploaded + delta));
        userRepository.save(user);
    }

    private List<String> sanitizeImageIds(List<String> imageIds) {
        if (imageIds == null) {
            return List.of();
        }

        return imageIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
    }

}
