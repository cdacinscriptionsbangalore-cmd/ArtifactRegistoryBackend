package com.cadac.stone_inscription.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReadPreference;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.entity.User;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    User findByEmail(String usernameFromToken);

    User findByAuthId(ObjectId userId);



}