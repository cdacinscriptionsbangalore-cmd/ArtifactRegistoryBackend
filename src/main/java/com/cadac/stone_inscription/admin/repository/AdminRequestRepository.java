package com.cadac.stone_inscription.admin.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.admin.entity.AdminRequest;

@Repository
public interface AdminRequestRepository extends MongoRepository<AdminRequest, ObjectId> {

    Optional<AdminRequest> findByEmail(String email);

    Optional<AdminRequest> findByApprovalTokenHash(String approvalTokenHash);
}
