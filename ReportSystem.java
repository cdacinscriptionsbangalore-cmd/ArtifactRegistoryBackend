import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
//  ENUMS
// ─────────────────────────────────────────────

enum Role { USER, HUMAN_MODERATOR, AI_MODERATOR, ADMIN }

enum ReportStatus { PENDING, AI_SCREENING, ESCALATED_TO_HUMAN, RESOLVED_AUTO, RESOLVED_HUMAN, DISMISSED }

enum ReportReason { SPAM, HATE_SPEECH, MISINFORMATION, HARASSMENT, EXPLICIT_CONTENT, OTHER }

enum ModerationAction { NONE, WARN, REMOVE_CONTENT, BAN_REPORTER, BAN_AUTHOR, ESCALATE, DISMISS }


// ─────────────────────────────────────────────
//  EXCEPTIONS
// ─────────────────────────────────────────────

class ReportValidationException extends RuntimeException {
    ReportValidationException(String msg) { super(msg); }
}

class ModerationException extends RuntimeException {
    ModerationException(String msg) { super(msg); }
}


// ─────────────────────────────────────────────
//  MARKER INTERFACE
// ─────────────────────────────────────────────

interface Reportable {
    String getId();
    String getAuthorId();
    String getContentType();   // "POST" | "COMMENT" | "USER"
}


// ─────────────────────────────────────────────
//  ENTITIES
// ─────────────────────────────────────────────

class User implements Reportable {
    private final String id;
    private final String name;
    private Role role;
    private boolean banned;
    private int reportCount;          // times this user has been reported
    private int reportsFiledCount;    // times this user has filed reports

    User(String id, String name, Role role) {
        this.id = id; this.name = name; this.role = role;
        this.banned = false;
    }

    @Override public String getId()          { return id; }
    @Override public String getAuthorId()    { return id; }
    @Override public String getContentType() { return "USER"; }

    public String getName()               { return name; }
    public Role getRole()                 { return role; }
    public boolean isBanned()             { return banned; }
    public int getReportCount()           { return reportCount; }
    public int getReportsFiledCount()     { return reportsFiledCount; }

    public void setBanned(boolean banned)            { this.banned = banned; }
    public void incrementReportCount()               { this.reportCount++; }
    public void incrementReportsFiledCount()         { this.reportsFiledCount++; }

    public boolean isModerator() {
        return role == Role.HUMAN_MODERATOR || role == Role.AI_MODERATOR || role == Role.ADMIN;
    }

    @Override public String toString() {
        return String.format("User{id='%s', name='%s', role=%s, banned=%s}", id, name, role, banned);
    }
}

class Post implements Reportable {
    private final String id;
    private final String authorId;
    private String content;
    private boolean removed;
    private final Instant createdAt;

    Post(String id, String authorId, String content) {
        this.id = id; this.authorId = authorId;
        this.content = content; this.removed = false;
        this.createdAt = Instant.now();
    }

    @Override public String getId()          { return id; }
    @Override public String getAuthorId()    { return authorId; }
    @Override public String getContentType() { return "POST"; }

    public String getContent()   { return content; }
    public boolean isRemoved()   { return removed; }
    public Instant getCreatedAt(){ return createdAt; }
    public void remove()         { this.removed = true; this.content = "[removed]"; }

    @Override public String toString() {
        // TODO: Remove the comments on the post
        // TODO: Remove the post, move the post to archive
        return String.format("Post{id='%s', content='%s', removed=%s}", id, content, removed );
    }
}

class Comment implements Reportable {
    private final String id;
    private final String authorId;
    private final String postId;
    private String description;
    private boolean removed;

    Comment(String id, String authorId, String postId, String description) {
        this.id = id; this.authorId = authorId;
        this.postId = postId; this.description = description;
        this.removed = false;
    }

    @Override public String getId()          { return id; }
    @Override public String getAuthorId()    { return authorId; }
    @Override public String getContentType() { return "COMMENT"; }

    public String getDescription() { return description; }
    public boolean isRemoved()     { return removed; }
    public void remove()           { 
        // TODO: Move comments to the archive
        this.removed = true; this.description = "[removed]"; 
    }

    @Override public String toString() {
        return String.format("Comment{id='%s', desc='%s', removed=%s}", id, description, removed);
    }
}


// ─────────────────────────────────────────────
//  REPORT
// ─────────────────────────────────────────────

class Report {
    private final String id;
    private final String reporterId;
    private final String targetId;
    private final String targetType;
    private final ReportReason reason;
    private final String details;
    private ReportStatus status;
    private ModerationAction actionTaken;
    private String resolvedBy;           // moderator id or "AI"
    private final Instant createdAt;
    private Instant resolvedAt;
    private double aiConfidenceScore;    // 0.0 – 1.0
    private final List<String> auditLog;

    Report(String id, String reporterId, Reportable target, ReportReason reason, String details) {
        this.id = id;
        this.reporterId = reporterId;
        this.targetId = target.getId();
        this.targetType = target.getContentType();
        this.reason = reason;
        this.details = details;
        this.status = ReportStatus.PENDING;
        this.actionTaken = ModerationAction.NONE;
        this.createdAt = Instant.now();
        this.auditLog = new ArrayList<>();
        addAuditEntry("Report created by reporter=" + reporterId);
    }

    // ── Getters ──
    public String getId()              { return id; }
    public String getReporterId()      { return reporterId; }
    public String getTargetId()        { return targetId; }
    public String getTargetType()      { return targetType; }
    public ReportReason getReason()    { return reason; }
    public String getDetails()         { return details; }
    public ReportStatus getStatus()    { return status; }
    public ModerationAction getActionTaken() { return actionTaken; }
    public double getAiConfidenceScore()     { return aiConfidenceScore; }
    public List<String> getAuditLog()        { return Collections.unmodifiableList(auditLog); }

    // ── Transitions (enforced — no arbitrary status jumps) ──
    public void transitionTo(ReportStatus newStatus, String actor, ModerationAction action) {
        validateTransition(this.status, newStatus);
        this.status = newStatus;
        this.actionTaken = action;
        this.resolvedBy = actor;
        if (isTerminal(newStatus)) this.resolvedAt = Instant.now();
        addAuditEntry(String.format("Status → %s | Action → %s | By → %s", newStatus, action, actor));
    }

    public void setAiConfidenceScore(double score) {
        this.aiConfidenceScore = score;
        addAuditEntry(String.format("AI confidence score set: %.2f", score));
    }

    private void validateTransition(ReportStatus from, ReportStatus to) {
        Map<ReportStatus, Set<ReportStatus>> allowed = new HashMap<>();
        allowed.put(ReportStatus.PENDING,             new HashSet<>(Arrays.asList(ReportStatus.AI_SCREENING, ReportStatus.DISMISSED)));
        allowed.put(ReportStatus.AI_SCREENING,        new HashSet<>(Arrays.asList(ReportStatus.RESOLVED_AUTO, ReportStatus.ESCALATED_TO_HUMAN)));
        allowed.put(ReportStatus.ESCALATED_TO_HUMAN,  new HashSet<>(Arrays.asList(ReportStatus.RESOLVED_HUMAN, ReportStatus.DISMISSED)));

        Set<ReportStatus> validNext = allowed.getOrDefault(from, Collections.emptySet());
        if (!validNext.contains(to)) {
            throw new ModerationException(
                String.format("Invalid transition: %s → %s", from, to));
        }
    }

    private boolean isTerminal(ReportStatus s) {
        return s == ReportStatus.RESOLVED_AUTO
            || s == ReportStatus.RESOLVED_HUMAN
            || s == ReportStatus.DISMISSED;
    }

    private void addAuditEntry(String entry) {
        auditLog.add(String.format("[%s] %s", Instant.now(), entry));
    }

    @Override public String toString() {
        return String.format(
            "Report{id='%s', target=%s(%s), reason=%s, status=%s, action=%s, aiScore=%.2f}",
            id, targetType, targetId, reason, status, actionTaken, aiConfidenceScore);
    }
}


// ─────────────────────────────────────────────
//  REPOSITORIES
// ─────────────────────────────────────────────

interface ReportRepository {
    Report save(Report report);
    Optional<Report> findById(String id);
    List<Report> findByStatus(ReportStatus status);
    boolean existsByReporterAndTarget(String reporterId, String targetId);
    List<Report> findAll();
}

class InMemoryReportRepository implements ReportRepository {
    private final Map<String, Report> store = new LinkedHashMap<>();

    @Override public Report save(Report r) { store.put(r.getId(), r); return r; }

    @Override public Optional<Report> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override public List<Report> findByStatus(ReportStatus status) {
        return store.values().stream()
            .filter(r -> r.getStatus() == status)
            .collect(Collectors.toList());
    }

    @Override public boolean existsByReporterAndTarget(String reporterId, String targetId) {
        return store.values().stream()
            .anyMatch(r -> r.getReporterId().equals(reporterId)
                       && r.getTargetId().equals(targetId));
    }

    @Override public List<Report> findAll() { return new ArrayList<>(store.values()); }
}

interface UserRepository {
    User save(User user);
    Optional<User> findById(String id);
    List<User> findAll();
}

class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> store = new LinkedHashMap<>();

    @Override public User save(User u) { store.put(u.getId(), u); return u; }
    @Override public Optional<User> findById(String id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<User> findAll() { return new ArrayList<>(store.values()); }
}

interface ContentRepository {
    void savePost(Post post);
    void saveComment(Comment comment);
    Optional<Post> findPostById(String id);
    Optional<Comment> findCommentById(String id);
}

class InMemoryContentRepository implements ContentRepository {
    private final Map<String, Post>    posts    = new LinkedHashMap<>();
    private final Map<String, Comment> comments = new LinkedHashMap<>();

    @Override public void savePost(Post p)        { posts.put(p.getId(), p); }
    @Override public void saveComment(Comment c)  { comments.put(c.getId(), c); }
    @Override public Optional<Post> findPostById(String id)       { return Optional.ofNullable(posts.get(id)); }
    @Override public Optional<Comment> findCommentById(String id) { return Optional.ofNullable(comments.get(id)); }
}


// ─────────────────────────────────────────────
//  SPECIFICATION PATTERN  (validation rules)
// ─────────────────────────────────────────────

interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
    String errorMessage();

    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }
}

class AndSpecification<T> implements Specification<T> {
    private final Specification<T> left;
    private final Specification<T> right;

    AndSpecification(Specification<T> left, Specification<T> right) {
        this.left = left; this.right = right;
    }

    @Override public boolean isSatisfiedBy(T t) {
        return left.isSatisfiedBy(t) && right.isSatisfiedBy(t);
    }

    @Override public String errorMessage() {
        return left.errorMessage() + " | " + right.errorMessage();
    }
}

// Payload carrying everything a spec might need to check
class ReportRequest {
    final User reporter;
    final Reportable target;
    final ReportRepository reportRepo;

    ReportRequest(User reporter, Reportable target, ReportRepository reportRepo) {
        this.reporter = reporter; this.target = target; this.reportRepo = reportRepo;
    }
}

class NotSelfReportSpec implements Specification<ReportRequest> {
    @Override public boolean isSatisfiedBy(ReportRequest r) {
        return !r.reporter.getId().equals(r.target.getAuthorId());
    }
    @Override public String errorMessage() { return "A user cannot report their own content."; }
}

class NotDuplicateReportSpec implements Specification<ReportRequest> {
    @Override public boolean isSatisfiedBy(ReportRequest r) {
        return !r.reportRepo.existsByReporterAndTarget(r.reporter.getId(), r.target.getId());
    }
    @Override public String errorMessage() { return "You have already reported this content."; }
}

class ReporterNotBannedSpec implements Specification<ReportRequest> {
    @Override public boolean isSatisfiedBy(ReportRequest r) { return !r.reporter.isBanned(); }
    @Override public String errorMessage() { return "Banned users cannot file reports."; }
}

class ModeratorCannotModerateOwnSpec implements Specification<ReportRequest> {
    @Override public boolean isSatisfiedBy(ReportRequest r) {
        // applies when the reporter is actually a moderator reviewing their own content
        if (r.reporter.isModerator()) {
            return !r.reporter.getId().equals(r.target.getAuthorId());
        }
        return true;
    }
    @Override public String errorMessage() { return "A moderator cannot moderate their own content."; }
}


// ─────────────────────────────────────────────
//  FACTORY
// ─────────────────────────────────────────────

class ReportFactory {
    private int counter = 1;

    public Report create(User reporter, Reportable target, ReportReason reason, String details) {
        String id = String.format("RPT-%04d", counter++);
        return new Report(id, reporter.getId(), target, reason, details);
    }
}


// ─────────────────────────────────────────────
//  MODERATOR INTERFACE + IMPLEMENTATIONS
// ─────────────────────────────────────────────

interface Moderator {
    ModerationAction screen(Report report, Reportable target, UserRepository userRepo, ContentRepository contentRepo);
    String getModeratorId();
}

// ── AI Moderator — scores and auto-resolves high-confidence cases ──
class AIModerator implements Moderator {
    private static final String ID = "AI_MOD";
    private static final double AUTO_RESOLVE_THRESHOLD = 0.85;

    @Override
    public ModerationAction screen(Report report, Reportable target,
                                   UserRepository userRepo, ContentRepository contentRepo) {
        double score = computeConfidenceScore(report, target);
        report.setAiConfidenceScore(score);

        if (score >= AUTO_RESOLVE_THRESHOLD) {
            return applyAction(report, target, userRepo, contentRepo);
        }
        // Not confident enough — escalate
        return ModerationAction.ESCALATE;
    }

    private double computeConfidenceScore(Report report, Reportable target) {
        // Simulated scoring based on reason severity + keyword detection
        double base = switch (report.getReason()) {
            case HATE_SPEECH      -> 0.80;
            case EXPLICIT_CONTENT -> 0.75;
            case HARASSMENT       -> 0.70;
            case SPAM             -> 0.60;
            case MISINFORMATION   -> 0.50;
            case OTHER            -> 0.30;
        };

        // Boost if content contains flagged keywords (simplified simulation)
        String content = getContent(target);
        if (content != null && containsFlaggedKeywords(content)) base += 0.15;

        return Math.min(base, 1.0);
    }

    private boolean containsFlaggedKeywords(String content) {
        List<String> flagged = Arrays.asList("hate", "spam", "fake", "scam", "explicit", "kill");
        String lower = content.toLowerCase();
        return flagged.stream().anyMatch(lower::contains);
    }

    private String getContent(Reportable target) {
        if (target instanceof Post)    return ((Post) target).getContent();
        if (target instanceof Comment) return ((Comment) target).getDescription();
        return null;
    }

    private ModerationAction applyAction(Report report, Reportable target,
                                         UserRepository userRepo, ContentRepository contentRepo) {
        // Remove content
        if (target instanceof Post)    ((Post) target).remove();
        if (target instanceof Comment) ((Comment) target).remove();

        // If author has 3+ reports, auto-ban
        userRepo.findById(target.getAuthorId()).ifPresent(author -> {
            author.incrementReportCount();
            if (author.getReportCount() >= 3) {
                author.setBanned(true);
            }
        });

        return ModerationAction.REMOVE_CONTENT;
    }

    @Override public String getModeratorId() { return ID; }
}

// ── Admin Moderator — handles escalated cases ──
class Admin implements Moderator {
    private final User moderatorUser;

    Admin(User moderatorUser) {
        if (!moderatorUser.isModerator()) {
            throw new ModerationException("User " + moderatorUser.getId() + " is not a moderator.");
        }
        this.moderatorUser = moderatorUser;
    }

    @Override
    public ModerationAction screen(Report report, Reportable target,
                                   UserRepository userRepo, ContentRepository contentRepo) {
        // Simulated human decision based on report reason + AI score
        ModerationAction decision = decideAction(report, target);

        switch (decision) {
            case REMOVE_CONTENT -> {
                if (target instanceof Post)    ((Post) target).remove();
                if (target instanceof Comment) ((Comment) target).remove();
                userRepo.findById(target.getAuthorId()).ifPresent(User::incrementReportCount);
            }
            case BAN_AUTHOR -> {
                if (target instanceof Post)    ((Post) target).remove();
                if (target instanceof Comment) ((Comment) target).remove();
                userRepo.findById(target.getAuthorId()).ifPresent(a -> {
                    a.setBanned(true);
                    a.incrementReportCount();
                });
            }
            case BAN_REPORTER -> {
                userRepo.findById(report.getReporterId()).ifPresent(r -> r.setBanned(true));
            }
            case DISMISS -> { /* no action on content */ }
            default -> { }
        }

        return decision;
    }

    private ModerationAction decideAction(Report report, Reportable target) {
        // Simulate human judgment: if AI was already fairly confident, remove content
        if (report.getAiConfidenceScore() >= 0.60) {
            return switch (report.getReason()) {
                case HATE_SPEECH, HARASSMENT -> ModerationAction.BAN_AUTHOR;
                case SPAM, EXPLICIT_CONTENT  -> ModerationAction.REMOVE_CONTENT;
                default                      -> ModerationAction.REMOVE_CONTENT;
            };
        }
        // Low-confidence + escalated → dismiss (reporter was wrong)
        return ModerationAction.DISMISS;
    }

    @Override public String getModeratorId() { return moderatorUser.getId(); }
}


// ─────────────────────────────────────────────
//  CHAIN OF RESPONSIBILITY  (moderation pipeline)
// ─────────────────────────────────────────────

abstract class ModerationHandler {
    protected ModerationHandler next;

    public ModerationHandler setNext(ModerationHandler next) {
        this.next = next;
        return next;                 // fluent chaining
    }

    public abstract void handle(Report report, Reportable target,
                                UserRepository userRepo, ContentRepository contentRepo,
                                ReportRepository reportRepo);

    protected void passToNext(Report report, Reportable target,
                               UserRepository userRepo, ContentRepository contentRepo,
                               ReportRepository reportRepo) {
        if (next != null) {
            next.handle(report, target, userRepo, contentRepo, reportRepo);
        } else {
            System.out.println("  [Chain] No further handler. Report left in current state: " + report.getStatus());
        }
    }
}

class AIScreeningHandler extends ModerationHandler {
    private final AIModerator aiModerator;

    AIScreeningHandler(AIModerator aiModerator) { this.aiModerator = aiModerator; }

    @Override
    public void handle(Report report, Reportable target,
                       UserRepository userRepo, ContentRepository contentRepo,
                       ReportRepository reportRepo) {
        System.out.println("  [AI Handler] Screening report " + report.getId() + "...");

        report.transitionTo(ReportStatus.AI_SCREENING, aiModerator.getModeratorId(), ModerationAction.NONE);
        ModerationAction action = aiModerator.screen(report, target, userRepo, contentRepo);

        System.out.printf("  [AI Handler] Score=%.2f | Decision=%s%n",
            report.getAiConfidenceScore(), action);

        if (action == ModerationAction.ESCALATE) {
            System.out.println("  [AI Handler] Confidence too low. Escalating to human moderator...");
            report.transitionTo(ReportStatus.ESCALATED_TO_HUMAN, aiModerator.getModeratorId(), ModerationAction.ESCALATE);
            reportRepo.save(report);
            passToNext(report, target, userRepo, contentRepo, reportRepo);
        } else {
            report.transitionTo(ReportStatus.RESOLVED_AUTO, aiModerator.getModeratorId(), action);
            reportRepo.save(report);
            System.out.println("  [AI Handler] Auto-resolved. Action taken: " + action);
        }
    }
}

class AdminModerationHandler extends ModerationHandler {
    private final Admin humanModerator;

    AdminModerationHandler(Admin humanModerator) { this.humanModerator = humanModerator; }

    @Override
    public void handle(Report report, Reportable target,
                       UserRepository userRepo, ContentRepository contentRepo,
                       ReportRepository reportRepo) {
        System.out.println("  [Human Handler] Moderator " + humanModerator.getModeratorId()
            + " reviewing escalated report " + report.getId() + "...");

        ModerationAction action = humanModerator.screen(report, target, userRepo, contentRepo);
        ReportStatus finalStatus = (action == ModerationAction.DISMISS)
            ? ReportStatus.DISMISSED
            : ReportStatus.RESOLVED_HUMAN;

        report.transitionTo(finalStatus, humanModerator.getModeratorId(), action);
        reportRepo.save(report);
        System.out.println("  [Human Handler] Decision: " + action + " → Status: " + finalStatus);
    }
}


// ─────────────────────────────────────────────
//  REPORT SERVICE  (facade — orchestrates everything)
// ─────────────────────────────────────────────

class ReportService {
    private final ReportRepository    reportRepo;
    private final UserRepository      userRepo;
    private final ContentRepository   contentRepo;
    private final ReportFactory       factory;
    private final Specification<ReportRequest> validationChain;
    private final ModerationHandler   moderationPipeline;

    ReportService(ReportRepository reportRepo,
                  UserRepository userRepo,
                  ContentRepository contentRepo,
                  ReportFactory factory,
                  ModerationHandler moderationPipeline) {
        this.reportRepo        = reportRepo;
        this.userRepo          = userRepo;
        this.contentRepo       = contentRepo;
        this.factory           = factory;
        this.moderationPipeline = moderationPipeline;

        // Compose validation rules
        this.validationChain = new NotSelfReportSpec()
            .and(new NotDuplicateReportSpec())
            .and(new ReporterNotBannedSpec())
            .and(new ModeratorCannotModerateOwnSpec());
    }

    /** Step 1 — user files a report */
    public Report fileReport(User reporter, Reportable target, ReportReason reason, String details) {
        ReportRequest req = new ReportRequest(reporter, target, reportRepo);

        // Run all specs; collect failures
        List<String> violations = new ArrayList<>();
        for (Specification<ReportRequest> spec : allSpecs()) {
            if (!spec.isSatisfiedBy(req)) violations.add(spec.errorMessage());
        }
        if (!violations.isEmpty()) {
            throw new ReportValidationException("Report rejected: " + String.join("; ", violations));
        }

        Report report = factory.create(reporter, target, reason, details);
        reporter.incrementReportsFiledCount();
        reportRepo.save(report);

        System.out.println("  [ReportService] Report filed: " + report.getId()
            + " by " + reporter.getName()
            + " against " + target.getContentType() + "(" + target.getId() + ")"
            + " for " + reason);

        return report;
    }

    /** Step 2 — trigger moderation pipeline for a report */
    public void startModeration(Report report) {
        Reportable target = resolveTarget(report);
        if (target == null) {
            throw new ModerationException("Target not found for report " + report.getId());
        }
        System.out.println("  [ReportService] Starting moderation pipeline for " + report.getId());
        moderationPipeline.handle(report, target, userRepo, contentRepo, reportRepo);
    }

    /** Convenience: file + moderate in one call */
    public Report fileAndModerate(User reporter, Reportable target, ReportReason reason, String details) {
        Report report = fileReport(reporter, target, reason, details);
        startModeration(report);
        return report;
    }

    public List<Report> getPendingReports() { return reportRepo.findByStatus(ReportStatus.PENDING); }
    public List<Report> getAllReports()      { return reportRepo.findAll(); }

    private Reportable resolveTarget(Report report) {
        return switch (report.getTargetType()) {
            case "POST"    -> contentRepo.findPostById(report.getTargetId()).map(p -> (Reportable) p).orElse(null);
            case "COMMENT" -> contentRepo.findCommentById(report.getTargetId()).map(c -> (Reportable) c).orElse(null);
            case "USER"    -> userRepo.findById(report.getTargetId()).map(u -> (Reportable) u).orElse(null);
            default        -> null;
        };
    }

    private List<Specification<ReportRequest>> allSpecs() {
        return Arrays.asList(
            new NotSelfReportSpec(),
            new NotDuplicateReportSpec(),
            new ReporterNotBannedSpec(),
            new ModeratorCannotModerateOwnSpec()
        );
    }
}


// ─────────────────────────────────────────────
//  MAIN — ENTRY POINT
// ─────────────────────────────────────────────

public class ReportSystem {

    // ── Shared state (injected everywhere) ──
    static ReportRepository   reportRepo;
    static UserRepository     userRepo;
    static ContentRepository  contentRepo;
    static ReportService      reportService;

    // ── Dummy data handles ──
    static User   alice, bob, carol, adminMod;
    static Post   post1, post2;
    static Comment comment1;

    // ─────────────────────────────────────────
    //  INIT
    // ─────────────────────────────────────────
    static void init() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  INITIALISING REPORT SYSTEM");
        System.out.println("═══════════════════════════════════════════════════");

        // Repositories
        reportRepo  = new InMemoryReportRepository();
        userRepo    = new InMemoryUserRepository();
        contentRepo = new InMemoryContentRepository();

        // Users
        alice    = new User("u001", "Alice",   Role.USER);
        bob      = new User("u002", "Bob",     Role.USER);
        carol    = new User("u003", "Carol",   Role.USER);
        adminMod = new User("u099", "AdminMod",Role.HUMAN_MODERATOR);

        userRepo.save(alice);
        userRepo.save(bob);
        userRepo.save(carol);
        userRepo.save(adminMod);

        // Posts
        post1 = new Post("p001", bob.getId(),   "Buy cheap followers now! Spam spam spam!");
        post2 = new Post("p002", carol.getId(), "Completely normal post about cooking.");

        contentRepo.savePost(post1);
        contentRepo.savePost(post2);

        // Comments
        comment1 = new Comment("c001", bob.getId(), post2.getId(),
            "This is fake news, scam alert!");
        contentRepo.saveComment(comment1);

        // Moderation pipeline: AI first → human if escalated
        AIModerator   ai    = new AIModerator();
        Admin human = new Admin(adminMod);

        AIScreeningHandler    aiHandler    = new AIScreeningHandler(ai);
        AdminModerationHandler humanHandler = new AdminModerationHandler(human);
        aiHandler.setNext(humanHandler);   // Chain of Responsibility wired up

        // Service (facade)
        reportService = new ReportService(
            reportRepo, userRepo, contentRepo,
            new ReportFactory(),
            aiHandler
        );

        System.out.println("  Users created    : " + userRepo.findAll().size());
        System.out.println("  Posts created    : 2  (post1 by Bob, post2 by Carol)");
        System.out.println("  Comments created : 1  (comment1 by Bob on post2)");
        System.out.println("  Moderators       : AdminMod (human), AI_MOD (automatic)");
        System.out.println();
    }


    // ─────────────────────────────────────────
    //  SCENARIO 1 — User creates a report
    // ─────────────────────────────────────────
    static String userCreatesAReport() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SCENARIO 1 : Alice reports Bob's spam post");
        System.out.println("───────────────────────────────────────────────────");

        try {
            // Alice reports post1 (authored by Bob) for spam
            Report report = reportService.fileReport(
                alice, post1, ReportReason.SPAM,
                "This post is clearly advertising spam and should be removed."
            );

            return String.format(
                "[OK] Report created → id=%s | target=%s(%s) | status=%s",
                report.getId(), report.getTargetType(), report.getTargetId(), report.getStatus()
            );

        } catch (ReportValidationException e) {
            return "[REJECTED] " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────
    //  SCENARIO 2 — Duplicate report attempt
    // ─────────────────────────────────────────
    static String userTriesToReportSamePostTwice() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SCENARIO 2 : Alice tries to report Bob's post again");
        System.out.println("───────────────────────────────────────────────────");

        try {
            reportService.fileReport(
                alice, post1, ReportReason.SPAM, "Reporting again."
            );
            return "[OK] Second report created (unexpected!)";
        } catch (ReportValidationException e) {
            return "[REJECTED] " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────
    //  SCENARIO 3 — Self-report attempt
    // ─────────────────────────────────────────
    static String userTriesToReportOwnContent() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SCENARIO 3 : Bob tries to report his own post");
        System.out.println("───────────────────────────────────────────────────");

        try {
            reportService.fileReport(
                bob, post1, ReportReason.OTHER, "I want to report myself."
            );
            return "[OK] Self-report created (unexpected!)";
        } catch (ReportValidationException e) {
            return "[REJECTED] " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────
    //  SCENARIO 4 — Content moderation starts (AI auto-resolves)
    // ─────────────────────────────────────────
    static String contentModerationStarts() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SCENARIO 4 : Moderation pipeline runs on pending reports");
        System.out.println("───────────────────────────────────────────────────");

        // Carol reports Bob's comment (contains flagged keywords → AI should be confident)
        Report commentReport = reportService.fileReport(
            carol, comment1, ReportReason.MISINFORMATION,
            "This comment spreads fake information."
        );

        System.out.println("  Running pipeline on comment report...");
        reportService.startModeration(commentReport);

        // Check outcome
        String commentStatus = String.format(
            "Comment report %s → status=%s | action=%s | removed=%s",
            commentReport.getId(), commentReport.getStatus(),
            commentReport.getActionTaken(), comment1.isRemoved()
        );

        // Also moderate the spam post filed by Alice in scenario 1
        List<Report> pending = reportRepo.findAll().stream()
            .filter(r -> r.getStatus() == ReportStatus.PENDING)
            .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder(commentStatus);
        for (Report r : pending) {
            System.out.println("\n  Running pipeline on report " + r.getId() + "...");
            reportService.startModeration(r);
            sb.append(String.format(
                "\n  Post report %s → status=%s | action=%s | post1 removed=%s",
                r.getId(), r.getStatus(), r.getActionTaken(), post1.isRemoved()
            ));
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────
    //  SCENARIO 5 — Escalated report (human moderator decides)
    // ─────────────────────────────────────────
    static String escalatedReportHandledByHuman() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SCENARIO 5 : Carol's post escalated to human moderator");
        System.out.println("───────────────────────────────────────────────────");

        // Alice reports Carol's benign post with a vague reason
        // AI score will be low → escalates to human → human dismisses
        Report report = reportService.fileReport(
            alice, post2, ReportReason.OTHER,
            "I just don't like this post."
        );

        System.out.println("  Running pipeline on low-confidence report...");
        reportService.startModeration(report);

        return String.format(
            "[OK] Report %s → status=%s | action=%s | post2 removed=%s | carol banned=%s",
            report.getId(), report.getStatus(), report.getActionTaken(),
            post2.isRemoved(), carol.isBanned()
        );
    }

    // ─────────────────────────────────────────
    //  SCENARIO 6 — Final system summary
    // ─────────────────────────────────────────
    static String systemSummary() {
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("  SYSTEM SUMMARY");
        System.out.println("───────────────────────────────────────────────────");

        StringBuilder sb = new StringBuilder();
        List<Report> all = reportService.getAllReports();

        sb.append(String.format("Total reports : %d%n", all.size()));
        for (Report r : all) {
            sb.append("  ").append(r).append("\n");
        }

        sb.append(String.format("%nUser states:%n"));
        for (User u : userRepo.findAll()) {
            sb.append(String.format("  %-12s | role=%-16s | banned=%-5s | timesReported=%d%n",
                u.getName(), u.getRole(), u.isBanned(), u.getReportCount()));
        }

        sb.append(String.format("%nContent states:%n"));
        sb.append(String.format("  post1    (Bob's spam post)        removed=%s%n", post1.isRemoved()));
        sb.append(String.format("  post2    (Carol's cooking post)   removed=%s%n", post2.isRemoved()));
        sb.append(String.format("  comment1 (Bob's fake-news comment) removed=%s%n", comment1.isRemoved()));

        return sb.toString();
    }


    // ─────────────────────────────────────────
    //  MAIN
    // ─────────────────────────────────────────
    public static void main(String[] args) {

        init();

        System.out.println(userCreatesAReport());
        System.out.println();

        System.out.println(userTriesToReportSamePostTwice());
        System.out.println();

        System.out.println(userTriesToReportOwnContent());
        System.out.println();

        System.out.println(contentModerationStarts());
        System.out.println();

        System.out.println(escalatedReportHandledByHuman());
        System.out.println();

        System.out.println(systemSummary());
    }
}