package com.cadac.stone_inscription.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.UserImage;
import com.cadac.stone_inscription.entity.UserImage.ImageType;

@Repository
public interface UserImageRepo extends MongoRepository<UserImage, String> {

    Optional<UserImage> findByUserIdAndImageType(ObjectId userId, ImageType imageType);

    void deleteByUserIdAndImageType(ObjectId userId, ImageType imageType);
}
