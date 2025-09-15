package com.cadac.stone_inscription.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.UserAuth;

@Repository
public interface UserAuthRepository extends MongoRepository<UserAuth, ObjectId> {

    UserAuth findByEmail(String email);

}
