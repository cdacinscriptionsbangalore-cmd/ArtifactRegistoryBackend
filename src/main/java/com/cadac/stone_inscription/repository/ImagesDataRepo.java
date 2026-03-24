package com.cadac.stone_inscription.repository;

import java.util.Collection;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.ImagesData;

@Repository
public interface ImagesDataRepo extends MongoRepository<ImagesData, String> {

    Optional<ImagesData> findFirstByMetadata_ImageHashValueAndPostIdIn(String imageHashValue,
            Collection<ObjectId> postIds);

}
