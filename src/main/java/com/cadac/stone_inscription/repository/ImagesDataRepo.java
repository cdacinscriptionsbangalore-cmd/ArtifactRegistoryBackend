package com.cadac.stone_inscription.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.ImagesData;

@Repository
public interface ImagesDataRepo extends MongoRepository<ImagesData, String> {

    Optional<ImagesData> findFirstByMetadata_ImageHashValue(String imageHashValue);

    List<ImagesData> findAllByMetadata_ImageHashValue(String imageHashValue);

}
