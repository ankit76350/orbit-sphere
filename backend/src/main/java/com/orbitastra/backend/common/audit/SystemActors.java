package com.orbitastra.backend.common.audit;

/**
 * Reserved values written into {@code createdByDocsId} when a document has no ordinary user
 * behind it.
 *
 * <p>Every document extends AuditedDocument, whose {@code createdByDocsId} is filled
 * automatically by Spring Data auditing. That works for the ordinary case — somebody is logged
 * in, and their account id goes on the row. Two cases are not ordinary, and both would
 * otherwise get a wrong answer silently.
 *
 * <p>**PLATFORM** is for a write with no tenant user, and provisioning a school is the reason
 * it exists. At the moment {@code POST /platform/schools} runs there is no UserAccount for that
 * school at all — the first one is created later. Without a sentinel the auditing hook has
 * nothing to write, and the first rows of every tenant would carry a null author.
 *
 * <p>**ANONYMOUS** is the more important one, and it is not needed yet. When feedback is built,
 * an anonymous submission must write this instead of the submitter's account id. If the ordinary
 * auditing path runs on those rows, the submitter's id is stored on a document the school
 * promised was anonymous — silently, in a field nothing on screen displays. See
 * {@code models/feedback/README.md}.
 *
 * <p>These are deliberately not valid ObjectId strings. A sentinel that looked like an id could
 * be joined against the accounts collection and quietly return nothing; one that obviously is
 * not an id forces the caller to handle it.
 */
public final class SystemActors {

    /** No tenant user existed. Used by platform provisioning. */
    public static final String PLATFORM = "SYSTEM_PLATFORM";

    /**
     * The actor is deliberately not recorded. Reserved for feedback, which is not built yet;
     * defined here so there is one place these values live.
     */
    public static final String ANONYMOUS = "ANONYMOUS";

    private SystemActors() {
    }

    /** True when a stored author is a sentinel rather than a UserAccount id. */
    public static boolean isSystem(String createdByDocsId) {
        return PLATFORM.equals(createdByDocsId) || ANONYMOUS.equals(createdByDocsId);
    }
}
