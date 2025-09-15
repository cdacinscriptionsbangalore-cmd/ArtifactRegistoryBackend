package com.cadac.stone_inscription.repository;


import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.InscriptionPost;

@Repository
public interface InscriptionPostRepo extends MongoRepository<InscriptionPost,ObjectId> {

    List<InscriptionPost> findByUserId(ObjectId postUserId);



}
