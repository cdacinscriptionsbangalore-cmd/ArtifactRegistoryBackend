package com.cadac.stone_inscription.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.PublicPostDescription;

@Repository
public interface PublicPostDescriptionRepo extends MongoRepository<PublicPostDescription,ObjectId>  {

    List<PublicPostDescription> findByPostId(ObjectId postId);

    void deleteAllByPostId(String postId);

    List<PublicPostDescription> findAllByUserId(ObjectId id);

}
