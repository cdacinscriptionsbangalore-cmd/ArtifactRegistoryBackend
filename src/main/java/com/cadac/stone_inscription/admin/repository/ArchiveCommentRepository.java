package com.cadac.stone_inscription.admin.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.admin.entity.ArchiveComment;

@Repository
public interface ArchiveCommentRepository extends MongoRepository<ArchiveComment, ObjectId> {
}
