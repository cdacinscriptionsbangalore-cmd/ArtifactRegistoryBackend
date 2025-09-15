package com.cadac.stone_inscription.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.ImagesData;

import dev.brachtendorf.jimagehash.hash.Hash;

@Repository
public interface ImagesDataRepo extends MongoRepository<ImagesData, String> {

          Optional<ImagesData> findFirstByMetadata_ImageHashValue(String imageHashValue);
   
}
