package com.cadac.stone_inscription.report.factory;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.report.dto.CreateReportRequest;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;

@Component
public class ReportFactory {

    public ModerationReport create(User reporter, ResolvedReportTarget target, CreateReportRequest request) {
        ModerationReport report = ModerationReport.builder()
                .id(new ObjectId())
                .reporterId(reporter.getId().toHexString())
                .targetId(target.getId())
                .targetType(target.getType())
                .targetAuthorId(target.getAuthorId())
                .reason(request.getReason())
                .details(request.getDetails().trim())
                .status(ReportStatus.PENDING)
                .actionTaken(ModerationAction.NONE)
                .aiConfidenceScore(0.0)
                .build();

        report.addAuditEntry(reporter.getId().toHexString(), "Report created");
        return report;
    }
}
