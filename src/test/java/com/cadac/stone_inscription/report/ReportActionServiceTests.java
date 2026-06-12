package com.cadac.stone_inscription.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import com.cadac.stone_inscription.content.delete.ContentDeleteResult;
import com.cadac.stone_inscription.content.delete.ContentDeleteService;
import com.cadac.stone_inscription.entity.InscriptionPost;
import com.cadac.stone_inscription.entity.PublicPostDescription;
import com.cadac.stone_inscription.entity.User;
import com.cadac.stone_inscription.entity.enums.PostStatus;
import com.cadac.stone_inscription.entity.model.Report;
import com.cadac.stone_inscription.report.entity.ModerationReport;
import com.cadac.stone_inscription.report.enums.ModerationAction;
import com.cadac.stone_inscription.report.enums.ReportReason;
import com.cadac.stone_inscription.report.enums.ReportStatus;
import com.cadac.stone_inscription.report.enums.ReportTargetType;
import com.cadac.stone_inscription.report.resolver.ResolvedReportTarget;
import com.cadac.stone_inscription.report.service.ReportActionService;
import com.cadac.stone_inscription.repository.InscriptionPostRepo;
import com.cadac.stone_inscription.repository.PublicPostDescriptionRepo;
import com.cadac.stone_inscription.repository.UserRepository;

class ReportActionServiceTests {

    @Test
    void markTargetUnderReviewUpdatesPostStatusAndReporterEntry() {
        SaveTracker<InscriptionPost> postTracker = new SaveTracker<>();
        ReportActionService service = new ReportActionService(
                postRepository(postTracker),
                commentRepository(new SaveTracker<>(), null),
                userRepository(null, new SaveTracker<>()),
                new TrackingDeleteService());

        InscriptionPost post = InscriptionPost.builder()
                .id(new ObjectId())
                .userId(new ObjectId())
                .status(PostStatus.ACCEPTED)
                .report(Report.builder().count(0).build())
                .build();

        User reporter = User.builder().id(new ObjectId()).name("Alice").build();
        ResolvedReportTarget target = ResolvedReportTarget.builder()
                .id(post.getId().toHexString())
                .authorId(post.getUserId().toHexString())
                .type(ReportTargetType.POST)
                .entity(post)
                .content("spam")
                .build();

        service.markTargetUnderReview(target, reporter, "looks suspicious");

        assertEquals(PostStatus.UNDER_REVIEW, post.getStatus());
        assertEquals(1, post.getReport().getReporters().size());
        assertEquals("looks suspicious", post.getReport().getReporters().get(0).getReason());
        assertEquals(post, postTracker.lastSaved);
    }

    @Test
    void warnActionRestoresAcceptanceAndIncrementsEmbeddedCount() {
        SaveTracker<InscriptionPost> postTracker = new SaveTracker<>();
        SaveTracker<User> userTracker = new SaveTracker<>();
        ObjectId authorId = new ObjectId();
        User author = User.builder().id(authorId).reportCount(4).blackListed(false).build();

        ReportActionService service = new ReportActionService(
                postRepository(postTracker),
                commentRepository(new SaveTracker<>(), null),
                userRepository(author, userTracker),
                new TrackingDeleteService());

        InscriptionPost post = InscriptionPost.builder()
                .id(new ObjectId())
                .userId(authorId)
                .status(PostStatus.UNDER_REVIEW)
                .report(Report.builder().count(2).build())
                .build();

        service.applyAction(
                baseReport(post.getId().toHexString(), authorId.toHexString(), ReportTargetType.POST),
                ResolvedReportTarget.builder()
                        .id(post.getId().toHexString())
                        .authorId(authorId.toHexString())
                        .type(ReportTargetType.POST)
                        .entity(post)
                        .content("content")
                        .build(),
                ModerationAction.WARN,
                "moderator",
                "first warning");

        assertEquals(PostStatus.ACCEPTED, post.getStatus());
        assertEquals(3, post.getReport().getCount());
        assertEquals(5, author.getReportCount());
        assertEquals(post, postTracker.lastSaved);
        assertEquals(author, userTracker.lastSaved);
    }

    @Test
    void removeContentActionIncrementsEmbeddedCommentCountAndDeletesComment() {
        SaveTracker<PublicPostDescription> commentTracker = new SaveTracker<>();
        SaveTracker<User> userTracker = new SaveTracker<>();
        TrackingDeleteService deleteService = new TrackingDeleteService();
        ObjectId authorId = new ObjectId();
        User author = User.builder().id(authorId).reportCount(0).blackListed(false).build();

        ReportActionService service = new ReportActionService(
                postRepository(new SaveTracker<>()),
                commentRepository(commentTracker, null),
                userRepository(author, userTracker),
                deleteService);

        PublicPostDescription comment = PublicPostDescription.builder()
                .id(new ObjectId())
                .userId(authorId)
                .status(PostStatus.UNDER_REVIEW)
                .report(Report.builder().count(0).build())
                .build();

        service.applyAction(
                baseReport(comment.getId().toHexString(), authorId.toHexString(), ReportTargetType.COMMENT),
                ResolvedReportTarget.builder()
                        .id(comment.getId().toHexString())
                        .authorId(authorId.toHexString())
                        .type(ReportTargetType.COMMENT)
                        .entity(comment)
                        .content("abusive")
                        .build(),
                ModerationAction.REMOVE_CONTENT,
                "AI_MODERATOR",
                null);

        assertEquals(1, comment.getReport().getCount());
        assertEquals(1, author.getReportCount());
        assertEquals(comment, commentTracker.lastSaved);
        assertEquals(author, userTracker.lastSaved);
        assertEquals(comment.getId(), deleteService.deletedCommentId);
    }

    @Test
    void dismissActionDoesNotIncrementEmbeddedCount() {
        SaveTracker<InscriptionPost> postTracker = new SaveTracker<>();
        ReportActionService service = new ReportActionService(
                postRepository(postTracker),
                commentRepository(new SaveTracker<>(), null),
                userRepository(null, new SaveTracker<>()),
                new TrackingDeleteService());

        ObjectId authorId = new ObjectId();
        InscriptionPost post = InscriptionPost.builder()
                .id(new ObjectId())
                .userId(authorId)
                .status(PostStatus.UNDER_REVIEW)
                .report(Report.builder().count(5).build())
                .build();

        service.applyAction(
                baseReport(post.getId().toHexString(), authorId.toHexString(), ReportTargetType.POST),
                ResolvedReportTarget.builder()
                        .id(post.getId().toHexString())
                        .authorId(authorId.toHexString())
                        .type(ReportTargetType.POST)
                        .entity(post)
                        .content("normal")
                        .build(),
                ModerationAction.DISMISS,
                "moderator",
                null);

        assertEquals(PostStatus.ACCEPTED, post.getStatus());
        assertEquals(5, post.getReport().getCount());
        assertEquals(post, postTracker.lastSaved);
    }

    @Test
    void userTargetRemoveContentDoesNotTriggerDeletion() {
        TrackingDeleteService deleteService = new TrackingDeleteService();
        SaveTracker<User> userTracker = new SaveTracker<>();
        ObjectId targetUserId = new ObjectId();
        User targetUser = User.builder().id(targetUserId).reportCount(1).blackListed(false).build();

        ReportActionService service = new ReportActionService(
                postRepository(new SaveTracker<>()),
                commentRepository(new SaveTracker<>(), null),
                userRepository(targetUser, userTracker),
                deleteService);

        service.applyAction(
                baseReport(targetUserId.toHexString(), targetUserId.toHexString(), ReportTargetType.USER),
                ResolvedReportTarget.builder()
                        .id(targetUserId.toHexString())
                        .authorId(targetUserId.toHexString())
                        .type(ReportTargetType.USER)
                        .entity(targetUser)
                        .content("profile bio")
                        .build(),
                ModerationAction.REMOVE_CONTENT,
                "AI_MODERATOR",
                null);

        assertEquals(2, targetUser.getReportCount());
        assertEquals(targetUser, userTracker.lastSaved);
        assertNull(deleteService.deletedPostId);
        assertNull(deleteService.deletedCommentId);
    }

    private ModerationReport baseReport(String targetId, String targetAuthorId, ReportTargetType targetType) {
        return ModerationReport.builder()
                .id(new ObjectId())
                .reporterId(new ObjectId().toHexString())
                .targetId(targetId)
                .targetType(targetType)
                .targetAuthorId(targetAuthorId)
                .reason(ReportReason.SPAM)
                .details("details")
                .status(ReportStatus.ESCALATED)
                .actionTaken(ModerationAction.ESCALATE)
                .build();
    }

    private InscriptionPostRepo postRepository(SaveTracker<InscriptionPost> tracker) {
        return (InscriptionPostRepo) Proxy.newProxyInstance(
                InscriptionPostRepo.class.getClassLoader(),
                new Class<?>[] { InscriptionPostRepo.class },
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        tracker.lastSaved = (InscriptionPost) args[0];
                        return args[0];
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private PublicPostDescriptionRepo commentRepository(
            SaveTracker<PublicPostDescription> tracker,
            PublicPostDescription foundComment) {
        return (PublicPostDescriptionRepo) Proxy.newProxyInstance(
                PublicPostDescriptionRepo.class.getClassLoader(),
                new Class<?>[] { PublicPostDescriptionRepo.class },
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        tracker.lastSaved = (PublicPostDescription) args[0];
                        return args[0];
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.ofNullable(foundComment);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private UserRepository userRepository(User foundUser, SaveTracker<User> tracker) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class },
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        tracker.lastSaved = (User) args[0];
                        return args[0];
                    }
                    if ("findById".equals(method.getName())) {
                        return Optional.ofNullable(foundUser);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) {
            return false;
        }
        if (returnType.equals(int.class)) {
            return 0;
        }
        if (returnType.equals(long.class)) {
            return 0L;
        }
        if (returnType.equals(Optional.class)) {
            return Optional.empty();
        }
        if (java.util.Collection.class.isAssignableFrom(returnType)) {
            return java.util.List.of();
        }
        return null;
    }

    private static class SaveTracker<T> {
        private T lastSaved;
    }

    private static class TrackingDeleteService extends ContentDeleteService {
        private ObjectId deletedPostId;
        private ObjectId deletedCommentId;

        TrackingDeleteService() {
            super(null, null, null, null, null, null, null, null);
        }

        @Override
        public ContentDeleteResult deletePost(ObjectId postId) {
            deletedPostId = postId;
            return ContentDeleteResult.builder().build();
        }

        @Override
        public ContentDeleteResult deleteComment(ObjectId commentId) {
            deletedCommentId = commentId;
            return ContentDeleteResult.builder().build();
        }
    }
}
