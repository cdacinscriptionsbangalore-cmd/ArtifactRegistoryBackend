package com.cadac.stone_inscription.report.repository;

import java.util.Collection;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;

@Repository
public interface ModerationReportRepository extends MongoRepository<ModerationReport, ObjectId> {

    List<ModerationReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<ModerationReport> findAllByOrderByCreatedAtDesc();

    boolean existsByReporterIdAndTargetIdAndTargetTypeAndStatusIn(
            String reporterId,
            String targetId,
            ReportTargetType targetType,
            Collection<ReportStatus> statuses);
}
