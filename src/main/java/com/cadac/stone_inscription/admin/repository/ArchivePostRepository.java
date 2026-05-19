package com.cadac.stone_inscription.admin.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.admin.entity.ArchivePost;

@Repository
public interface ArchivePostRepository extends MongoRepository<ArchivePost, ObjectId> {
}
