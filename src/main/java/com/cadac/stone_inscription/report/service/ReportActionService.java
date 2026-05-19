package com.cadac.stone_inscription.report.service;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.content.delete.ContentDeleteService;
import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.enums.PostStatus;
import com.cadac.stone_inscription.entity.model.ReportEntry;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.repository.InscriptionPostRepo;
import com.cadac.stone_inscription.repository.PublicPostDescriptionRepo;
import com.cadac.stone_inscription.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportActionService {

    private final InscriptionPostRepo inscriptionPostRepo;
    private final PublicPostDescriptionRepo publicPostDescriptionRepo;
    private final UserRepository userRepository;
    private final ContentDeleteService contentDeleteService;

    public void markTargetUnderReview(ResolvedReportTarget target, User reporter, String details) {
        if (target.getType() == ReportTargetType.POST) {
            InscriptionPost post = (InscriptionPost) target.getEntity();
            post.setStatus(PostStatus.UNDER_REVIEW);
            if (post.getReport() != null && post.getReport().getReporters() != null) {
                post.getReport().getReporters().add(buildReportEntry(reporter, details));
            }
            inscriptionPostRepo.save(post);
            return;
        }

        if (target.getType() == ReportTargetType.COMMENT) {
            PublicPostDescription comment = (PublicPostDescription) target.getEntity();
            comment.setStatus(PostStatus.UNDER_REVIEW);
            if (comment.getReport() != null && comment.getReport().getReporters() != null) {
                comment.getReport().getReporters().add(buildReportEntry(reporter, details));
            }
            publicPostDescriptionRepo.save(comment);
        }
    }

    public void applyAction(
            ModerationReport report,
            ResolvedReportTarget target,
            ModerationAction action,
            String actor,
            String note) {

        switch (action) {
            case NONE, ESCALATE -> {
                return;
            }
            case WARN -> {
                incrementAuthorReportCount(target.getAuthorId(), false);
                incrementValidatedTargetReportCount(target);
                restoreTargetAcceptance(target);
                report.addAuditEntry(actor, appendNote("Warned target author", note));
            }
            case REMOVE_CONTENT -> {
                incrementValidatedTargetReportCount(target);
                removeTargetContent(target);
                incrementAuthorReportCount(target.getAuthorId(), false);
                report.addAuditEntry(actor, appendNote("Removed reported target content", note));
            }
            case BAN_AUTHOR -> {
                incrementValidatedTargetReportCount(target);
                removeTargetContent(target);
                incrementAuthorReportCount(target.getAuthorId(), true);
                report.addAuditEntry(actor, appendNote("Banned target author", note));
            }
            case BAN_REPORTER -> {
                blackListUser(report.getReporterId());
                restoreTargetAcceptance(target);
                report.addAuditEntry(actor, appendNote("Blacklisted reporter", note));
            }
            case DISMISS -> {
                restoreTargetAcceptance(target);
                report.addAuditEntry(actor, appendNote("Dismissed report", note));
            }
        }
    }

    private ReportEntry buildReportEntry(User reporter, String details) {
        return ReportEntry.builder()
                .userId(reporter.getId().toHexString())
                .name(reporter.getName())
                .reason(details)
                .build();
    }

    private void removeTargetContent(ResolvedReportTarget target) {
        if (target.getType() == ReportTargetType.USER) {
            return;
        }

        if (target.getType() == ReportTargetType.POST) {
            contentDeleteService.deletePost(new ObjectId(target.getId()));
            return;
        }

        if (target.getType() == ReportTargetType.COMMENT) {
            contentDeleteService.deleteComment(new ObjectId(target.getId()));
        }
    }

    private void restoreTargetAcceptance(ResolvedReportTarget target) {
        if (target.getType() == ReportTargetType.USER) {
            return;
        }

        if (target.getType() == ReportTargetType.POST) {
            InscriptionPost post = (InscriptionPost) target.getEntity();
            post.setStatus(PostStatus.ACCEPTED);
            inscriptionPostRepo.save(post);
            return;
        }

        if (target.getType() == ReportTargetType.COMMENT) {
            PublicPostDescription comment = (PublicPostDescription) target.getEntity();
            comment.setStatus(PostStatus.ACCEPTED);
            publicPostDescriptionRepo.save(comment);
        }
    }

    private void incrementAuthorReportCount(String userId, boolean blackList) {
        if (!ObjectId.isValid(userId)) {
            return;
        }

        userRepository.findById(new ObjectId(userId)).ifPresent(user -> {
            int currentCount = user.getReportCount() == null ? 0 : user.getReportCount();
            user.setReportCount(currentCount + 1);
            if (blackList) {
                user.setBlackListed(true);
            }
            userRepository.save(user);
        });
    }

    private void incrementValidatedTargetReportCount(ResolvedReportTarget target) {
        if (target.getType() == ReportTargetType.POST) {
            InscriptionPost post = (InscriptionPost) target.getEntity();
            if (post.getReport() != null) {
                int currentCount = post.getReport().getCount() == null ? 0 : post.getReport().getCount();
                post.getReport().setCount(currentCount + 1);
                inscriptionPostRepo.save(post);
            }
            return;
        }

        if (target.getType() == ReportTargetType.COMMENT) {
            PublicPostDescription comment = (PublicPostDescription) target.getEntity();
            if (comment.getReport() != null) {
                int currentCount = comment.getReport().getCount() == null ? 0 : comment.getReport().getCount();
                comment.getReport().setCount(currentCount + 1);
                publicPostDescriptionRepo.save(comment);
            }
        }
    }

    private void blackListUser(String userId) {
        if (!ObjectId.isValid(userId)) {
            return;
        }

        userRepository.findById(new ObjectId(userId)).ifPresent(user -> {
            user.setBlackListed(true);
            userRepository.save(user);
        });
    }

    private String appendNote(String baseMessage, String note) {
        if (note == null || note.isBlank()) {
            return baseMessage;
        }
        return baseMessage + " | " + note.trim();
    }
}
